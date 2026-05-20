package io.github.xuse.romking.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.metadata.ee.Game;
import io.github.xuse.romking.metadata.ee.GameListService;
import io.github.xuse.romking.metadata.ee.Gamelist;
import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.enums.WrapType;
import io.github.xuse.romking.repo.obj.MediaFile;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.service.RomExportOptions;
import lombok.extern.slf4j.Slf4j;

/**
 * ROM导出任务。
 * 将ARCHIVE仓库中的ROM文件导出到TF卡目标目录，
 * 同时生成EE格式的gamelist.xml。
 * 
 * MD5校验逻辑：
 * - 目标卡上有同名文件时，计算目标文件MD5与源ROM的MD5比对
 * - MD5一致：无需复制，直接跳过
 * - MD5不一致但主仓库同名游戏其他版本含该MD5：提示有不同版本，跳过
 * - MD5不一致且主仓库无该MD5：提示发现新版本，不覆盖，留记录待人工核实
 */
@Slf4j
public class ExportRomTask implements Task {

	private final RomExportOptions options;
	private final RomDirRepository romDirRepo;
	private final RomFileRepository romFileRepo;
	private final MediaFileRepository mediaRepo;
	private final GameListService gameListService;

	private long begin;
	private String progress = "";

	// 统计
	private int totalFiles = 0;
	private int copiedFiles = 0;
	private int skippedFiles = 0;
	private int md5MatchSkipped = 0;
	private int versionMismatchSkipped = 0;
	private int newVersionFound = 0;
	private int failedFiles = 0;
	private int deletedFiles = 0;
	private final List<String> failedDetails = new ArrayList<>();
	private final List<String> newVersionDetails = new ArrayList<>();

	public ExportRomTask(RomExportOptions options,
			RomDirRepository romDirRepo, RomFileRepository romFileRepo,
			MediaFileRepository mediaRepo, GameListService gameListService) {
		this.options = options;
		this.romDirRepo = romDirRepo;
		this.romFileRepo = romFileRepo;
		this.mediaRepo = mediaRepo;
		this.gameListService = gameListService;
	}

	@Override
	public TaskType getType() {
		return TaskType.EXPORT;
	}

	@Override
	public String getName() {
		return "导出到 " + options.getTargetPath();
	}

	@Override
	public String getProgress() {
		return progress;
	}

	@Override
	public long getBegin() {
		return begin;
	}

	@Override
	public ProcessResult execute() {
		this.begin = System.currentTimeMillis();
		try {
			doExport();
			StringBuilder msg = new StringBuilder();
			msg.append(String.format("导出完成: 共%d个文件, 复制%d, 跳过%d",
					totalFiles, copiedFiles, skippedFiles + md5MatchSkipped));
			if (md5MatchSkipped > 0) {
				msg.append(String.format(", MD5一致%d", md5MatchSkipped));
			}
			if (versionMismatchSkipped > 0) {
				msg.append(String.format(", 不同版本%d", versionMismatchSkipped));
			}
			if (newVersionFound > 0) {
				msg.append(String.format(", 发现新版本%d", newVersionFound));
			}
			if (failedFiles > 0) {
				msg.append(String.format(", 失败%d", failedFiles));
			}
			if (deletedFiles > 0) {
				msg.append(String.format(", 删除%d", deletedFiles));
			}

			int code = 200;
			if (failedFiles > 0) code = 201;
			if (newVersionFound > 0) code = 202;

			ProcessResult result = new ProcessResult(code, msg.toString());
			List<String> allDetails = new ArrayList<>();
			allDetails.addAll(newVersionDetails);
			allDetails.addAll(failedDetails);
			if (!allDetails.isEmpty()) {
				result.setDetails(allDetails);
			}
			return result;
		} catch (Exception e) {
			log.error("导出任务异常", e);
			return new ProcessResult(500, "导出异常: " + e.getMessage());
		}
	}

	private void doExport() {
		File targetRoot = new File(options.getTargetPath());
		if (!targetRoot.exists()) {
			targetRoot.mkdirs();
		}

		for (int dirId : options.getSourceDirIds()) {
			RomDir sourceDir = romDirRepo.load(dirId);
			if (sourceDir == null) {
				log.warn("源目录不存在: dirId={}", dirId);
				continue;
			}
			exportDir(sourceDir, targetRoot);
		}
	}

	/**
	 * 导出单个源目录到目标
	 */
	private void exportDir(RomDir sourceDir, File targetRoot) {
		Platform platform = sourceDir.getPlatform();
		String platformDirName = platform.name().toLowerCase();
		File targetPlatformDir = new File(targetRoot, platformDirName);
		if (!targetPlatformDir.exists()) {
			targetPlatformDir.mkdirs();
		}

		progress = "正在导出: " + platformDirName;

		// 获取源目录下所有ROM文件
		List<RomFile> romFiles = romFileRepo.find(q ->
				q.where(RomFileRepository.dirId.eq(sourceDir.getId())));

		// 获取源目录下所有媒体文件
		List<MediaFile> mediaFiles = mediaRepo.find(q ->
				q.where(MediaFileRepository.dirId.eq(sourceDir.getId())));

		totalFiles += romFiles.size();

		// 导出ROM文件
		Set<String> exportedRomPaths = new HashSet<>();
		List<Game> gameEntries = new ArrayList<>();

		for (RomFile romFile : romFiles) {
			String filepath = romFile.getFilepath();
			exportedRomPaths.add(filepath);

			File targetFile = new File(targetPlatformDir, filepath);

			// 目标文件已存在的处理
			if (targetFile.isFile()) {
				ExistingFileAction action = handleExistingFile(romFile, targetFile, sourceDir);
				switch (action) {
					case SKIP_MD5_MATCH:
						md5MatchSkipped++;
						gameEntries.add(toGameEntry(romFile));
						continue;
					case SKIP_DIFFERENT_VERSION:
						versionMismatchSkipped++;
						gameEntries.add(toGameEntry(romFile));
						continue;
					case SKIP_NEW_VERSION:
						newVersionFound++;
						gameEntries.add(toGameEntry(romFile));
						continue;
					case COPY:
						// 继续执行复制
						break;
				}
			}

			// 复制ROM文件
			File sourceFile = new File(sourceDir.getRootpath(), filepath);
			if (copyFile(sourceFile, targetFile, romFile)) {
				copiedFiles++;
				gameEntries.add(toGameEntry(romFile));
			} else {
				failedFiles++;
			}
		}

		// 导出媒体文件
		Map<String, String> mediaPathMap = new HashMap<>();
		for (MediaFile media : mediaFiles) {
			File sourceMedia = new File(sourceDir.getRootpath(), media.getFilepath());
			File targetMedia = new File(targetPlatformDir, media.getFilepath());
			if (sourceMedia.isFile() && !targetMedia.isFile()) {
				copyFile(sourceMedia, targetMedia, null);
			}
			mediaPathMap.put(media.getFilepath(), media.getFilepath());
		}

		// 覆盖模式：删除目标中多余的ROM文件
		if (options.isOverwrite()) {
			deleteExtraFiles(targetPlatformDir, targetPlatformDir, exportedRomPaths, mediaPathMap);
		}

		// 生成gamelist.xml
		generateGamelist(targetPlatformDir, gameEntries);

		progress = String.format("已完成: %s (复制%d, 跳过%d, 失败%d)",
				platformDirName, copiedFiles, skippedFiles + md5MatchSkipped, failedFiles);
	}

	/**
	 * 处理目标已存在同名文件的情况
	 */
	private ExistingFileAction handleExistingFile(RomFile romFile, File targetFile, RomDir sourceDir) {
		// 快速导出模式：不校验MD5，直接跳过
		if (options.isQuickExport()) {
			skippedFiles++;
			return ExistingFileAction.SKIP_MD5_MATCH;
		}

		// 计算目标文件的MD5
		String targetMd5 = computeRomMd5(targetFile, romFile);
		if (targetMd5 == null) {
			// 无法计算MD5，执行复制覆盖
			return ExistingFileAction.COPY;
		}

		String sourceMd5 = romFile.getMd5();
		if (sourceMd5 == null || sourceMd5.isEmpty()) {
			// 源文件没有MD5记录，执行复制
			return ExistingFileAction.COPY;
		}

		// MD5一致，无需复制
		if (sourceMd5.equalsIgnoreCase(targetMd5)) {
			return ExistingFileAction.SKIP_MD5_MATCH;
		}

		// MD5不一致，检查主仓库中同名游戏的其他版本
		String gameName = romFile.getName();
		List<RomFile> sameNameRoms = romFileRepo.find(q ->
				q.where(RomFileRepository.name.eq(gameName)));

		// 检查主仓库中是否有其他版本包含该MD5
		boolean knownVersion = false;
		for (RomFile other : sameNameRoms) {
			if (other.getId() != romFile.getId() && targetMd5.equalsIgnoreCase(other.getMd5())) {
				knownVersion = true;
				break;
			}
		}

		if (knownVersion) {
			// 主仓库中有该MD5对应的其他版本
			log.info("[{}] 目标卡上存在同游戏的不同版本(已知), MD5={}", gameName, targetMd5);
			return ExistingFileAction.SKIP_DIFFERENT_VERSION;
		} else {
			// 发现新版本，不覆盖，留记录
			String detail = String.format("[新版本] %s - 目标MD5=%s, 源MD5=%s, 文件=%s",
					gameName, targetMd5, sourceMd5, targetFile.getAbsolutePath());
			newVersionDetails.add(detail);
			log.info(detail);
			return ExistingFileAction.SKIP_NEW_VERSION;
		}
	}

	/**
	 * 计算目标ROM文件的MD5。
	 * 如果是ZIP包装，需要读取ZIP内主文件的MD5。
	 */
	private String computeRomMd5(File file, RomFile romFile) {
		if (romFile.getWrapType() == WrapType.ZIPPED_ROM || romFile.getWrapType() == WrapType.ZIPPED_DIRECTORY) {
			// ZIP文件，读取内部主ROM的MD5
			return computeZipInnerMd5(file, romFile.getRomName());
		} else {
			return computeFileMd5(file);
		}
	}

	/**
	 * 计算ZIP内指定文件的MD5
	 */
	private String computeZipInnerMd5(File zipFile, String innerFileName) {
		try (ZipFile zf = new ZipFile(zipFile)) {
			ZipEntry entry = zf.getEntry(innerFileName);
			if (entry == null) {
				// 找不到指定文件名，取最大的文件
				entry = zf.stream()
						.filter(e -> !e.isDirectory())
						.max((a, b) -> Long.compare(a.getSize(), b.getSize()))
						.orElse(null);
			}
			if (entry == null) {
				return null;
			}
			try (InputStream is = zf.getInputStream(entry)) {
				return computeStreamMd5(is);
			}
		} catch (IOException e) {
			log.warn("读取ZIP计算MD5失败: {}", zipFile.getAbsolutePath(), e);
			return null;
		}
	}

	private enum ExistingFileAction {
		/** MD5一致，跳过 */
		SKIP_MD5_MATCH,
		/** 不同版本但主仓库已知，跳过 */
		SKIP_DIFFERENT_VERSION,
		/** 发现新版本，不覆盖 */
		SKIP_NEW_VERSION,
		/** 需要复制 */
		COPY
	}

	/**
	 * 复制单个文件
	 */
	private boolean copyFile(File source, File target, RomFile romFile) {
		if (!source.isFile()) {
			String msg = (romFile != null ? romFile.getName() : source.getName()) + " - 源文件不存在: " + source.getAbsolutePath();
			failedDetails.add(msg);
			log.warn(msg);
			return false;
		}
		try {
			Path targetPath = target.toPath();
			Files.createDirectories(targetPath.getParent());
			Files.copy(source.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException e) {
			String msg = (romFile != null ? romFile.getName() : source.getName()) + " - 复制失败: " + e.getMessage();
			failedDetails.add(msg);
			log.warn(msg, e);
			return false;
		}
	}

	/**
	 * 覆盖模式下删除目标中多余的文件
	 */
	private void deleteExtraFiles(File baseDir, File currentDir,
			Set<String> keepRomPaths, Map<String, String> keepMediaPaths) {
		File[] files = currentDir.listFiles();
		if (files == null) return;
		for (File file : files) {
			if (file.isDirectory()) {
				deleteExtraFiles(baseDir, file, keepRomPaths, keepMediaPaths);
				String[] children = file.list();
				if (children != null && children.length == 0) {
					file.delete();
				}
			} else {
				String rel = normalizeSlash(getRelativePath(file, baseDir));
				if (file.getName().equals("gamelist.xml")) {
					continue;
				}
				if (!keepRomPaths.contains(rel) && !keepMediaPaths.containsKey(rel)) {
					file.delete();
					deletedFiles++;
				}
			}
		}
	}

	/**
	 * 将RomFile转换为EE格式的Game条目
	 */
	private Game toGameEntry(RomFile romFile) {
		Game game = new Game();
		game.setPath("./" + normalizeSlash(romFile.getFilepath()));
		game.setName(romFile.getName());

		if (romFile.getMedias() != null) {
			Object image = romFile.getMedias().get("image");
			if (image != null) {
				game.setImage(image.toString());
			}
			Object video = romFile.getMedias().get("video");
			if (video != null) {
				game.setVideo(video.toString());
			}
			Object thumbnail = romFile.getMedias().get("thumbnail");
			if (thumbnail != null) {
				game.setThumbnail(thumbnail.toString());
			}
		}

		if (romFile.getVersion() != null) {
			game.setDesc(romFile.getVersion());
		}
		if (romFile.getGameid() != null && !romFile.getGameid().isEmpty()) {
			game.setGenre(romFile.getGameid());
		}
		return game;
	}

	/**
	 * 生成gamelist.xml
	 */
	private void generateGamelist(File platformDir, List<Game> games) {
		if (games.isEmpty()) {
			return;
		}
		File gamelistFile = new File(platformDir, "gamelist.xml");
		Gamelist gamelist = new Gamelist(games);
		try {
			gameListService.saveXml(gamelist, gamelistFile);
		} catch (Exception e) {
			log.error("生成gamelist.xml失败: {}", gamelistFile.getAbsolutePath(), e);
			failedDetails.add("gamelist.xml生成失败: " + e.getMessage());
		}
	}

	private String getRelativePath(File file, File baseDir) {
		String filePath = file.getAbsolutePath();
		String basePath = baseDir.getAbsolutePath();
		if (filePath.startsWith(basePath)) {
			String rel = filePath.substring(basePath.length());
			if (rel.startsWith(File.separator)) {
				rel = rel.substring(1);
			}
			return rel;
		}
		return file.getName();
	}

	private static String normalizeSlash(String path) {
		return path.replace('\\', '/');
	}

	private String computeFileMd5(File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			return computeStreamMd5(fis);
		} catch (IOException e) {
			log.warn("计算MD5失败: {}", file.getAbsolutePath(), e);
			return null;
		}
	}

	private String computeStreamMd5(InputStream is) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] buffer = new byte[8192];
			int read;
			while ((read = is.read(buffer)) != -1) {
				md.update(buffer, 0, read);
			}
			byte[] digest = md.digest();
			StringBuilder sb = new StringBuilder(32);
			for (byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException | IOException e) {
			log.warn("计算MD5失败", e);
			return null;
		}
	}
}
