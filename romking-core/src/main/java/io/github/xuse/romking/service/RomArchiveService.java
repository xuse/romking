package io.github.xuse.romking.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.xuse.querydsl.util.Assert;

import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.enums.RepoType;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.tasks.ArchiveRomTask;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;

@Service
public class RomArchiveService {

	@Inject
	private GlobalTaskService taskService;

	@Inject
	private RomDirRepository romDirRepo;

	@Inject
	private RomFileRepository romFileRepo;

	@Inject
	private MediaFileRepository mediaRepo;

	/**
	 * 提交归档任务（按目录ID）
	 *
	 * @param sourceDirId 源目录ID（INSTANCE类型）
	 * @param targetDirId 目标目录ID（ARCHIVE类型）
	 */
	public void archive(int sourceDirId, int targetDirId) {
		RomDir source = romDirRepo.load(sourceDirId);
		RomDir target = romDirRepo.load(targetDirId);
		Assert.notNull(source, "源目录不存在: " + sourceDirId);
		Assert.notNull(target, "目标目录不存在: " + targetDirId);
		Assert.isTrue(target.getType() == RepoType.ARCHIVE,
				"目标目录必须是ARCHIVE类型: " + target.getRootpath());

		ArchiveRomTask task = new ArchiveRomTask(sourceDirId, targetDirId,
				romDirRepo, romFileRepo, mediaRepo);
		taskService.submit(task);
	}

	/**
	 * 按label批量归档。
	 * 将源INSTANCE仓库（指定label）的所有平台目录，
	 * 按platform自动匹配目标ARCHIVE仓库中对应的目录进行归档。
	 * 如果目标ARCHIVE中没有对应platform的目录，自动创建。
	 *
	 * @param sourceLabel 源INSTANCE仓库的label
	 * @param targetLabel 目标ARCHIVE仓库的label
	 */
	public void archiveByLabel(String sourceLabel, String targetLabel) {
		Assert.notNull(sourceLabel, "源仓库label不能为空");
		Assert.notNull(targetLabel, "目标仓库label不能为空");

		// 查出源label下所有INSTANCE目录
		List<RomDir> sourceDirs = romDirRepo.find(q -> q.where(
				RomDirRepository.t.label.eq(sourceLabel),
				RomDirRepository.t.type.eq(RepoType.INSTANCE)));
		Assert.isTrue(sourceDirs != null && !sourceDirs.isEmpty(), "源仓库无INSTANCE目录: " + sourceLabel);

		// 查出目标label下所有ARCHIVE目录，按platform建Map
		List<RomDir> targetDirs = romDirRepo.find(q -> q.where(
				RomDirRepository.t.label.eq(targetLabel),
				RomDirRepository.t.type.eq(RepoType.ARCHIVE)));
		Map<String, RomDir> targetByPlatform = new HashMap<>();
		for (RomDir dir : targetDirs) {
			targetByPlatform.put(dir.getPlatform().name(), dir);
		}

		// 遍历源目录，逐个提交归档任务
		for (RomDir sourceDir : sourceDirs) {
			String platformKey = sourceDir.getPlatform().name();
			RomDir targetDir = targetByPlatform.get(platformKey);

			// 如果目标没有该platform的目录，自动创建
			if (targetDir == null) {
				targetDir = new RomDir();
				targetDir.setLabel(targetLabel);
				targetDir.setPlatform(sourceDir.getPlatform());
				targetDir.setRootpath(buildArchiveRootpath(targetLabel, sourceDir.getPlatform().name()));
				targetDir.setDescription("自动创建");
				targetDir.setType(RepoType.ARCHIVE);
				romDirRepo.insert(targetDir);
				targetByPlatform.put(platformKey, targetDir);
			}

			ArchiveRomTask task = new ArchiveRomTask(sourceDir.getId(), targetDir.getId(),
					romDirRepo, romFileRepo, mediaRepo);
			taskService.submit(task);
		}
	}

	/**
	 * 构建ARCHIVE目录的默认rootpath
	 */
	private String buildArchiveRootpath(String label, String platformName) {
		// 使用目标label下已有目录的父路径推断，或使用默认规则
		List<RomDir> existing = romDirRepo.find(q -> q.where(
				RomDirRepository.t.label.eq(label)));
		if (!existing.isEmpty()) {
			String existingPath = existing.get(0).getRootpath();
			java.io.File parent = new java.io.File(existingPath).getParentFile();
			if (parent != null) {
				return new java.io.File(parent, platformName.toLowerCase()).getAbsolutePath();
			}
		}
		// 兜底：使用label/platform
		return label + "/" + platformName.toLowerCase();
	}

	/**
	 * 获取所有INSTANCE类型的目录（供UI选择源）
	 */
	public List<RomDir> listInstanceDirs() {
		return romDirRepo.find(q -> q.where(RomDirRepository.t.type.eq(RepoType.INSTANCE)));
	}

	/**
	 * 获取所有ARCHIVE类型的目录（供UI选择目标）
	 */
	public List<RomDir> listArchiveDirs() {
		return romDirRepo.find(q -> q.where(RomDirRepository.t.type.eq(RepoType.ARCHIVE)));
	}

	/**
	 * 获取所有不重复的INSTANCE仓库label
	 */
	public List<String> listInstanceLabels() {
		return romDirRepo.getFactory()
				.select(RomDirRepository.t.label)
				.distinct()
				.from(RomDirRepository.t)
				.where(RomDirRepository.t.type.eq(RepoType.INSTANCE))
				.fetch();
	}

	/**
	 * 获取所有不重复的ARCHIVE仓库label
	 */
	public List<String> listArchiveLabels() {
		return romDirRepo.getFactory()
				.select(RomDirRepository.t.label)
				.distinct()
				.from(RomDirRepository.t)
				.where(RomDirRepository.t.type.eq(RepoType.ARCHIVE))
				.fetch();
	}
}
