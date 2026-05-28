package io.github.xuse.romking.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.github.xuse.querydsl.util.Assert;
import com.github.xuse.querydsl.util.IOUtils;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.metadata.ee.Game;
import io.github.xuse.romking.metadata.ee.GameListService;
import io.github.xuse.romking.metadata.ee.Gamelist;
import io.github.xuse.romking.nointro.DatManageService;
import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.enums.MediaType;
import io.github.xuse.romking.repo.enums.RepoType;
import io.github.xuse.romking.repo.enums.WrapType;
import io.github.xuse.romking.repo.obj.KnownRom;
import io.github.xuse.romking.repo.obj.MediaFile;
import io.github.xuse.romking.repo.obj.QRomFile;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.service.RomScanOptions;
import lombok.extern.slf4j.Slf4j;

/**
 * ROM目录扫描任务。
 * 扫描一个根目录（TF卡或ROM集合），遍历其下的平台子目录，
 * 将ROM文件和媒体文件录入数据库。
 */
@Slf4j
public class ScanRomTask implements Task {
	private final File rootDir;
	private final RomScanOptions options;
	private final RomDirRepository romDirRepo;
	private final RomFileRepository romFileRepo;
	private final MediaFileRepository mediaRepo;
	private final GameListService gameListService;
	private final DatManageService datManageService;

	private long begin;
	private volatile TaskProgress taskProgress = new TaskProgress("等待开始", 0, 0);

	// 扫描统计
	private int totalDirs = 0;
	private int dirCount = 0;
	private int romCount = 0;
	private int mediaCount = 0;
	private int skippedCount = 0;
	private int matchedCount = 0;

	// 媒体文件扩展名映射
	private static final Map<String, MediaType> MEDIA_EXT_MAP = new HashMap<>();
	static {
		MEDIA_EXT_MAP.put("mp3", MediaType.SOUND);
		MEDIA_EXT_MAP.put("ogg", MediaType.SOUND);
		MEDIA_EXT_MAP.put("wav", MediaType.SOUND);
		MEDIA_EXT_MAP.put("mp4", MediaType.VIDEO);
		MEDIA_EXT_MAP.put("avi", MediaType.VIDEO);
		MEDIA_EXT_MAP.put("flv", MediaType.VIDEO);
		MEDIA_EXT_MAP.put("mkv", MediaType.VIDEO);
		MEDIA_EXT_MAP.put("jpeg", MediaType.IMAGE);
		MEDIA_EXT_MAP.put("jpg", MediaType.IMAGE);
		MEDIA_EXT_MAP.put("png", MediaType.IMAGE);
		MEDIA_EXT_MAP.put("bmp", MediaType.IMAGE);
		MEDIA_EXT_MAP.put("gif", MediaType.IMAGE);
	}

	// 应忽略的文件扩展名
	private static final java.util.Set<String> IGNORE_EXTS = java.util.Set.of(
			"txt", "md", "xml", "ini", "cfg", "log", "db", "fs", "nfo");

	public ScanRomTask(File rootDir, RomScanOptions options,
			RomDirRepository romDirRepo, RomFileRepository romFileRepo,
			MediaFileRepository mediaRepo, GameListService gameListService,
			DatManageService datManageService) {
		this.rootDir = rootDir;
		this.options = options;
		this.romDirRepo = romDirRepo;
		this.romFileRepo = romFileRepo;
		this.mediaRepo = mediaRepo;
		this.gameListService = gameListService;
		this.datManageService = datManageService;
		Assert.isTrue(rootDir.isDirectory(), "扫描路径必须是目录: " + rootDir);
	}

	@Override
	public TaskType getType() {
		return TaskType.SCAN_DIR;
	}

	@Override
	public String getName() {
		return options.getLabel() + "<" + rootDir.getAbsolutePath() + ">";
	}

	@Override
	public TaskProgress getTaskProgress() {
		return taskProgress;
	}

	@Override
	public long getBegin() {
		return begin;
	}

	@Override
	public ProcessResult execute() {
		this.begin = System.currentTimeMillis();
		try {
			scanRoot();
			String msg = String.format("扫描完成: %d个目录, %d个ROM, %d个媒体文件, %d个跳过, %d个匹配ROM数据库",
					dirCount, romCount, mediaCount, skippedCount, matchedCount);
			taskProgress = new TaskProgress(msg, totalDirs, totalDirs);
			return new ProcessResult(200, msg);
		} catch (Exception e) {
			log.error("扫描任务异常", e);
			return new ProcessResult(500, "扫描异常: " + e.getMessage());
		}
	}

	/**
	 * 扫描根目录，遍历子目录作为平台目录
	 */
	private void scanRoot() {
		File[] subDirs = rootDir.listFiles(File::isDirectory);
		if (subDirs == null) {
			return;
		}
		// 计算有效目录总数（排除隐藏和系统目录）
		for (File subDir : subDirs) {
			if (!subDir.getName().startsWith(".") && !subDir.getName().startsWith("_")) {
				totalDirs++;
			}
		}
		taskProgress = new TaskProgress("开始扫描", 0, totalDirs);
		for (File subDir : subDirs) {
			// 跳过隐藏目录和系统目录
			if (subDir.getName().startsWith(".") || subDir.getName().startsWith("_")) {
				continue;
			}
			scanPlatformDir(subDir);
		}
	}

	/**
	 * 扫描一个平台目录（如 gba/, nes/ 等）
	 */
	private void scanPlatformDir(File platformDir) {
		// 推断平台
		Platform platform = options.getPlatform();
		if (platform == null) {
			platform = guessPlatform(platformDir.getName());
		}
		if (platform == null) {
			log.warn("无法识别平台目录: {}, 跳过", platformDir.getName());
			skippedCount++;
			return;
		}

		// 检查是否有gamelist.xml
		File gamelistFile = new File(platformDir, "gamelist.xml");
		boolean hasGamelist = gamelistFile.isFile();

		if (!hasGamelist && !options.isScanWithoutGamelist()) {
			log.info("目录 {} 无gamelist.xml且选项不允许扫描，跳过", platformDir.getName());
			skippedCount++;
			return;
		}

		taskProgress = new TaskProgress("正在扫描: " + platformDir.getName(), dirCount, totalDirs);

		// 创建或更新 RomDir 记录
		RomDir romDir = createOrUpdateRomDir(platformDir, platform);
		int dirId = romDir.getId();
		dirCount++;

		// 解析gamelist获取元数据
		Map<String, Game> gameMetadata = new HashMap<>();
		if (hasGamelist) {
			try {
				Gamelist gamelist = gameListService.loadXml(gamelistFile);
				for (Game game : gamelist.getGames()) {
					if (game.getPath() != null) {
						String normalizedPath = normalizePath(game.getPath());
						gameMetadata.put(normalizedPath, game);
					}
				}
			} catch (Exception e) {
				log.warn("解析gamelist.xml失败: {}", gamelistFile.getAbsolutePath(), e);
			}
		}

		// 扫描目录中的文件
		scanFilesInDir(platformDir, platformDir, dirId, platform, gameMetadata);

		// 更新最后扫描时间
		romDirRepo.getFactory().update(RomDirRepository.t)
				.set(RomDirRepository.t.lastScan, new Date())
				.where(RomDirRepository.t.id.eq(romDir.getId()))
				.execute();
	}

	/**
	 * 创建或查找已有的RomDir记录。
	 * 增量模式下查找已有记录；非增量模式下总是创建新记录。
	 */
	private RomDir createOrUpdateRomDir(File platformDir, Platform platform) {
		// 增量模式：先查找已有记录
		if (options.isIncremental()) {
			List<RomDir> existing = romDirRepo.find(q -> q.where(
					RomDirRepository.t.label.eq(options.getLabel()),
					RomDirRepository.t.rootpath.eq(platformDir.getAbsolutePath())));
			if (!existing.isEmpty()) {
				return existing.get(0);
			}
		}
		RomDir romDir = new RomDir();
		romDir.setLabel(options.getLabel());
		romDir.setPlatform(platform);
		romDir.setRootpath(platformDir.getAbsolutePath());
		romDir.setDescription("");
		romDir.setType(options.getRepoType());
		romDirRepo.insert(romDir);
		return romDir;
	}

	/**
	 * 递归扫描目录中的文件
	 */
	private void scanFilesInDir(File baseDir, File currentDir, int dirId,
			Platform platform, Map<String, Game> gameMetadata) {
		File[] files = currentDir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				// 递归扫描子目录（media目录等）
				scanFilesInDir(baseDir, file, dirId, platform, gameMetadata);
				continue;
			}

			String fileName = file.getName();
			// 跳过隐藏文件
			if (fileName.startsWith(".")) {
				continue;
			}

			String ext = IOUtils.getExtName(fileName).toLowerCase();

			// 忽略的文件类型
			if (IGNORE_EXTS.contains(ext)) {
				continue;
			}

			// 媒体文件
			MediaType mediaType = MEDIA_EXT_MAP.get(ext);
			if (mediaType != null) {
				addMediaFile(file, baseDir, dirId, ext, mediaType);
				continue;
			}

			// ROM文件
			addRomFile(file, baseDir, dirId, platform, ext, gameMetadata);
		}
	}

	/**
	 * 添加媒体文件记录
	 */
	private void addMediaFile(File file, File baseDir, int dirId, String ext, MediaType mediaType) {
		String relativePath = getRelativePath(file, baseDir);
		MediaFile media = new MediaFile();
		media.setDirId(dirId);
		media.setFilepath(relativePath);
		media.setExt(ext);
		media.setType(mediaType);
		media.setMd5("");
		media.setReferCount(0);
		mediaRepo.insert(media);
		mediaCount++;
	}

	/**
	 * 添加ROM文件记录
	 */
	private void addRomFile(File file, File baseDir, int dirId,
			Platform platform, String ext, Map<String, Game> gameMetadata) {
		String relativePath = getRelativePath(file, baseDir);

		// 增量扫描：检查是否已有记录且文件未变更
		if (options.isIncremental()) {
			List<RomFile> existing = romFileRepo.find(q -> q.where(
					RomFileRepository.dirId.eq(dirId),
					RomFileRepository.filepath.eq(relativePath)));
			if (!existing.isEmpty()) {
				RomFile existingRom = existing.get(0);
				long fileSize = file.length();
				long fileModified = file.lastModified();
				// size + lastModified 一致 → 文件未变更，跳过
				if (existingRom.getLength() == fileSize
						&& existingRom.getRomModified() != null
						&& existingRom.getRomModified().getTime() == fileModified) {
					skippedCount++;
					return;
				}
				// 文件有变更，删除旧记录后重新入库
				romFileRepo.getFactory().delete(QRomFile.romFile)
						.where(QRomFile.romFile.id.eq(existingRom.getId()))
						.execute();
			}
		}

		String normalizedPath = "./" + relativePath.replace('\\', '/');

		// 查找元数据
		Game metadata = gameMetadata.get(normalizePath(normalizedPath));

		RomFile romFile = new RomFile();
		romFile.setDirId(dirId);
		romFile.setFilepath(relativePath);
		romFile.setPlatform(platform);
		romFile.setRomModified(new Date(file.lastModified()));

		// 处理ZIP文件
		boolean isZip = "zip".equalsIgnoreCase(ext);
		if (isZip) {
			fillZipRomInfo(file, romFile);
		} else {
			romFile.setWrapType(WrapType.ROM);
			romFile.setRomName(file.getName());
			romFile.setRomExt(ext);
			romFile.setLength(file.length());
			romFile.setZippedFiles(0);
			// 计算MD5
			if (options.isComputeMd5()) {
				romFile.setMd5(computeFileMd5(file));
			}
			// 计算CRC
			if (options.isComputeCrc()) {
				romFile.setCrc(computeFileCrc(file));
			}
		}

		// 从文件名提取游戏名（去掉扩展名）
		String romName = romFile.getRomName();
		String gameName = IOUtils.removeExt(romName != null ? romName : file.getName());
		romFile.setName(gameName);
		romFile.setGameid("");

		// 用gamelist元数据补全信息
		if (metadata != null) {
			applyMetadata(romFile, metadata);
		}

		// 用No-Intro ROM数据库补全信息
		applyKnownRomInfo(romFile);

		// 默认值
		if (romFile.getRegion() == null) {
			romFile.setRegion(guessRegion(romFile.getName()));
		}
		if (romFile.getWrapType() == null) {
			romFile.setWrapType(WrapType.ROM);
		}

		romFileRepo.insert(romFile);
		romCount++;
	}

	/**
	 * 填充ZIP包装的ROM信息
	 */
	private void fillZipRomInfo(File zipFile, RomFile romFile) {
		try (ZipFile zf = new ZipFile(zipFile)) {
			List<ZipEntry> entries = new ArrayList<>();
			zf.stream().filter(e -> !e.isDirectory()).forEach(entries::add);

			if (entries.isEmpty()) {
				romFile.setWrapType(WrapType.ZIPPED_ROM);
				romFile.setRomName(zipFile.getName());
				romFile.setRomExt("zip");
				romFile.setLength(zipFile.length());
				romFile.setZippedFiles(0);
				return;
			}

			romFile.setZippedFiles(entries.size());

			if (entries.size() == 1) {
				// 单文件ZIP
				romFile.setWrapType(WrapType.ZIPPED_ROM);
				ZipEntry entry = entries.get(0);
				romFile.setRomName(entry.getName());
				romFile.setRomExt(IOUtils.getExtName(entry.getName()).toLowerCase());
				romFile.setLength(entry.getSize());

				// CRC可以直接从ZIP头获取
				if (options.isComputeCrc() && entry.getCrc() != -1) {
					romFile.setCrc(Long.toHexString(entry.getCrc()));
				}
				// MD5需要读取内容
				if (options.isComputeMd5()) {
					try (InputStream is = zf.getInputStream(entry)) {
						romFile.setMd5(computeStreamMd5(is));
					}
				}
			} else {
				// 多文件ZIP
				romFile.setWrapType(WrapType.ZIPPED_DIRECTORY);
				// 找最大的文件作为主ROM
				ZipEntry mainEntry = entries.stream()
						.max((a, b) -> Long.compare(a.getSize(), b.getSize()))
						.orElse(entries.get(0));
				romFile.setRomName(mainEntry.getName());
				romFile.setRomExt(IOUtils.getExtName(mainEntry.getName()).toLowerCase());
				romFile.setLength(mainEntry.getSize());

				if (options.isComputeCrc() && mainEntry.getCrc() != -1) {
					romFile.setCrc(Long.toHexString(mainEntry.getCrc()));
				}
				if (options.isComputeMd5()) {
					try (InputStream is = zf.getInputStream(mainEntry)) {
						romFile.setMd5(computeStreamMd5(is));
					}
				}
			}
		} catch (IOException e) {
			log.warn("读取ZIP文件失败: {}", zipFile.getAbsolutePath(), e);
			romFile.setWrapType(WrapType.ZIPPED_ROM);
			romFile.setRomName(zipFile.getName());
			romFile.setRomExt("zip");
			romFile.setLength(zipFile.length());
			romFile.setZippedFiles(0);
		}
	}

	/**
	 * 将gamelist.xml中的元数据应用到RomFile
	 */
	private void applyMetadata(RomFile romFile, Game game) {
		if (game.getName() != null && !game.getName().isBlank()) {
			romFile.setDisplayName(game.getName());
		}
		if (game.getDesc() != null) {
			romFile.setVersion(game.getDesc().length() > 256
					? game.getDesc().substring(0, 256) : game.getDesc());
		}
		if (game.getGenre() != null) {
			romFile.setGameid(game.getGenre());
		}
		// 媒体信息存入medias字段
		Map<String, Object> medias = new HashMap<>();
		if (game.getImage() != null) {
			medias.put("image", game.getImage());
		}
		if (game.getVideo() != null) {
			medias.put("video", game.getVideo());
		}
		if (game.getThumbnail() != null) {
			medias.put("thumbnail", game.getThumbnail());
		}
		if (game.getMarquee() != null) {
			medias.put("marquee", game.getMarquee());
		}
		if (!medias.isEmpty()) {
			romFile.setMedias(medias);
		}
	}

	/**
	 * 用No-Intro ROM数据库（known_rom表）补全RomFile信息。
	 * 优先用MD5匹配，其次用CRC+size匹配。
	 * 
	 * 匹配成功后：
	 * - 不覆盖name字段（保留gamelist或文件名来源的中文名）
	 * - 设置gameid为parentName（游戏家族标识，用于去重和版本管理）
	 * - 补全region（如果当前为空）
	 */
	private void applyKnownRomInfo(RomFile romFile) {
		if (datManageService == null) return;

		KnownRom known = null;

		// 优先MD5匹配
		String md5 = romFile.getMd5();
		if (md5 != null && !md5.isEmpty()) {
			known = datManageService.findByMd5(md5);
		}

		// MD5未匹配，尝试CRC+size
		if (known == null) {
			String crc = romFile.getCrc();
			if (crc != null && !crc.isEmpty() && romFile.getLength() > 0) {
				var candidates = datManageService.findByCrcAndSize(crc, romFile.getLength());
				if (candidates.size() == 1) {
					known = candidates.get(0);
				}
			}
		}

		if (known == null) return;

		// 匹配成功
		matchedCount++;

		// 设置gameid为parentName（游戏家族标识），用于去重和版本关联
		if (known.getParentName() != null && !known.getParentName().isEmpty()) {
			romFile.setGameid(known.getParentName());
		} else if (known.getGameName() != null && !known.getGameName().isEmpty()) {
			romFile.setGameid(known.getGameName());
		}

		// 用No-Intro标准名设置name字段
		String noIntroName = known.getGameName();
		if (noIntroName != null && !noIntroName.isEmpty()) {
			String cleanName = noIntroName.replaceAll("\\s*\\([^)]*\\)\\s*", "").trim();
			if (!cleanName.isEmpty()) {
				romFile.setName(cleanName);
			}
		}

		// 补全region（仅在当前为空时）
		if (romFile.getRegion() == null && known.getRegion() != null) {
			romFile.setRegion(known.getRegion());
		}
	}

	/**
	 * 从目录名推断平台
	 */
	private Platform guessPlatform(String dirName) {
		String name = dirName.toUpperCase().replace("-", "").replace("_", "").replace(" ", "");
		for (Platform p : Platform.values()) {
			if (name.equals(p.name().toUpperCase())) {
				return p;
			}
		}
		// 常见别名
		Map<String, Platform> aliases = Map.ofEntries(
				Map.entry("FC", Platform.NES),
				Map.entry("FAMICOM", Platform.NES),
				Map.entry("SFC", Platform.SNES),
				Map.entry("SUPERNES", Platform.SNES),
				Map.entry("SUPERFAMICOM", Platform.SNES),
				Map.entry("SUPERNINTENDO", Platform.SNES),
				Map.entry("NINTENDO64", Platform.N64),
				Map.entry("GAMECUBE", Platform.NGC),
				Map.entry("GAMEBOY", Platform.GB),
				Map.entry("GAMEBOYCOLOR", Platform.GBC),
				Map.entry("GAMEBOYADVANCE", Platform.GBA),
				Map.entry("NINTENDODS", Platform.NDS),
				Map.entry("MEGADRIVE", Platform.MD),
				Map.entry("GENESIS", Platform.MD),
				Map.entry("SATURN", Platform.SS),
				Map.entry("DREAMCAST", Platform.DC),
				Map.entry("PLAYSTATION", Platform.PS),
				Map.entry("PS1", Platform.PS),
				Map.entry("PSX", Platform.PS),
				Map.entry("PLAYSTATION2", Platform.PS2)
		);
		return aliases.get(name);
	}

	/**
	 * 从文件名猜测区域
	 */
	private io.github.xuse.romking.repo.enums.Region guessRegion(String name) {
		if (name == null) {
			return io.github.xuse.romking.repo.enums.Region.OTHER;
		}
		String upper = name.toUpperCase();
		if (upper.contains("(J)") || upper.contains("(JPN)") || upper.contains("(JAPAN)")) {
			return io.github.xuse.romking.repo.enums.Region.JPN;
		}
		if (upper.contains("(U)") || upper.contains("(USA)")) {
			return io.github.xuse.romking.repo.enums.Region.USA;
		}
		if (upper.contains("(E)") || upper.contains("(EUR)") || upper.contains("(EUROPE)")) {
			return io.github.xuse.romking.repo.enums.Region.EUR;
		}
		if (upper.contains("(CN)") || upper.contains("(CHN)") || upper.contains("(中)") || containsChinese(name)) {
			return io.github.xuse.romking.repo.enums.Region.CHN;
		}
		if (upper.contains("(K)") || upper.contains("(KOR)") || upper.contains("(KOREA)")) {
			return io.github.xuse.romking.repo.enums.Region.KOR;
		}
		return io.github.xuse.romking.repo.enums.Region.OTHER;
	}

	/**
	 * 判断字符串是否包含中文字符
	 */
	private boolean containsChinese(String str) {
		for (char c : str.toCharArray()) {
			if (c >= 0x4E00 && c <= 0x9FFF) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 规范化路径（去掉 ./ 和 ../ 前缀，统一分隔符）
	 */
	private static String normalizePath(String path) {
		path = path.replace('\\', '/');
		if (path.startsWith("../")) {
			path = path.substring(3);
		}
		if (path.startsWith("./")) {
			path = path.substring(2);
		}
		return path;
	}

	/**
	 * 获取相对路径
	 */
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

	/**
	 * 计算文件MD5
	 */
	private String computeFileMd5(File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			return computeStreamMd5(fis);
		} catch (IOException e) {
			log.warn("计算MD5失败: {}", file.getAbsolutePath(), e);
			return null;
		}
	}

	/**
	 * 计算流的MD5
	 */
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

	/**
	 * 计算文件CRC32
	 */
	private String computeFileCrc(File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = fis.read(buffer)) != -1) {
				crc32.update(buffer, 0, read);
			}
			return Long.toHexString(crc32.getValue());
		} catch (IOException e) {
			log.warn("计算CRC失败: {}", file.getAbsolutePath(), e);
			return null;
		}
	}
}
