package io.github.xuse.romking.tasks;

import java.util.ArrayList;
import java.util.List;

import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.enums.RepoType;
import io.github.xuse.romking.repo.obj.MediaFile;
import io.github.xuse.romking.repo.obj.QRomFile;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.obj.RomFile;
import lombok.extern.slf4j.Slf4j;

/**
 * ROM归档任务。
 * 将INSTANCE仓库中的ROM记录合并到ARCHIVE仓库，基于MD5去重。
 * 仅操作数据库记录，不移动物理文件。
 */
@Slf4j
public class ArchiveRomTask implements Task {

	private final int sourceDirId;
	private final int targetDirId;
	private final RomDirRepository romDirRepo;
	private final RomFileRepository romFileRepo;
	private final MediaFileRepository mediaRepo;

	private long begin;
	private String progress = "";

	// 统计
	private int totalRoms = 0;
	private int archivedRoms = 0;
	private int duplicateSkipped = 0;
	private int mediaArchived = 0;
	private final List<String> details = new ArrayList<>();

	public ArchiveRomTask(int sourceDirId, int targetDirId,
			RomDirRepository romDirRepo, RomFileRepository romFileRepo,
			MediaFileRepository mediaRepo) {
		this.sourceDirId = sourceDirId;
		this.targetDirId = targetDirId;
		this.romDirRepo = romDirRepo;
		this.romFileRepo = romFileRepo;
		this.mediaRepo = mediaRepo;
	}

	@Override
	public TaskType getType() {
		return TaskType.ARCHIVE;
	}

	@Override
	public String getName() {
		return "归档 dirId=" + sourceDirId + " → dirId=" + targetDirId;
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
			doArchive();
			String msg = String.format("归档完成: 共%d个ROM, 归档%d, 重复跳过%d, 媒体%d",
					totalRoms, archivedRoms, duplicateSkipped, mediaArchived);
			ProcessResult result = new ProcessResult(200, msg);
			if (!details.isEmpty()) {
				result.setDetails(details);
			}
			return result;
		} catch (Exception e) {
			log.error("归档任务异常", e);
			return new ProcessResult(500, "归档异常: " + e.getMessage());
		}
	}

	private void doArchive() {
		// 校验源目录和目标目录
		RomDir sourceDir = romDirRepo.load(sourceDirId);
		RomDir targetDir = romDirRepo.load(targetDirId);
		if (sourceDir == null || targetDir == null) {
			throw new IllegalStateException("源目录或目标目录不存在");
		}
		if (targetDir.getType() != RepoType.ARCHIVE) {
			throw new IllegalStateException("目标目录必须是ARCHIVE类型");
		}

		progress = "正在归档: " + sourceDir.getLabel() + "/" + sourceDir.getPlatform();

		// 获取源目录下所有ROM
		List<RomFile> sourceRoms = romFileRepo.find(q ->
				q.where(RomFileRepository.dirId.eq(sourceDirId)));
		totalRoms = sourceRoms.size();

		// 获取目标目录下已有ROM的MD5集合
		List<RomFile> targetRoms = romFileRepo.find(q ->
				q.where(RomFileRepository.dirId.eq(targetDirId)));
		java.util.Set<String> targetMd5Set = new java.util.HashSet<>();
		for (RomFile r : targetRoms) {
			if (r.getMd5() != null && !r.getMd5().isEmpty()) {
				targetMd5Set.add(r.getMd5().toLowerCase());
			}
		}

		// 逐个归档
		for (RomFile rom : sourceRoms) {
			String md5 = rom.getMd5();

			// 基于MD5去重
			if (md5 != null && !md5.isEmpty() && targetMd5Set.contains(md5.toLowerCase())) {
				duplicateSkipped++;
				details.add("[重复] " + rom.getName() + " MD5=" + md5);
				continue;
			}

			// 复制记录到目标目录（修改dirId）
			RomFile archived = copyRomFile(rom, targetDirId);
			romFileRepo.insert(archived);
			archivedRoms++;

			// 记录MD5避免后续重复
			if (md5 != null && !md5.isEmpty()) {
				targetMd5Set.add(md5.toLowerCase());
			}
		}

		// 归档媒体文件
		List<MediaFile> sourceMedias = mediaRepo.find(q ->
				q.where(MediaFileRepository.dirId.eq(sourceDirId)));
		for (MediaFile media : sourceMedias) {
			MediaFile archived = copyMediaFile(media, targetDirId);
			mediaRepo.insert(archived);
			mediaArchived++;
		}

		progress = String.format("归档完成: 归档%d, 重复%d", archivedRoms, duplicateSkipped);
	}

	/**
	 * 复制RomFile记录（修改dirId，清除自增ID）
	 */
	private RomFile copyRomFile(RomFile source, int newDirId) {
		RomFile copy = new RomFile();
		copy.setDirId(newDirId);
		copy.setFilepath(source.getFilepath());
		copy.setWrapType(source.getWrapType());
		copy.setGameid(source.getGameid());
		copy.setName(source.getName());
		copy.setGameType(source.getGameType());
		copy.setRegion(source.getRegion());
		copy.setZippedFiles(source.getZippedFiles());
		copy.setVersion(source.getVersion());
		copy.setHackComment(source.getHackComment());
		copy.setPlatform(source.getPlatform());
		copy.setRomName(source.getRomName());
		copy.setRomExt(source.getRomExt());
		copy.setRomModified(source.getRomModified());
		copy.setLength(source.getLength());
		copy.setCrc(source.getCrc());
		copy.setMd5(source.getMd5());
		copy.setMedias(source.getMedias());
		copy.setFavorite(source.getFavorite());
		return copy;
	}

	/**
	 * 复制MediaFile记录
	 */
	private MediaFile copyMediaFile(MediaFile source, int newDirId) {
		MediaFile copy = new MediaFile();
		copy.setDirId(newDirId);
		copy.setFilepath(source.getFilepath());
		copy.setReferCount(source.getReferCount());
		copy.setExt(source.getExt());
		copy.setType(source.getType());
		copy.setMd5(source.getMd5());
		return copy;
	}
}
