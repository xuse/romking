package io.github.xuse.romking.service;

import java.util.List;

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
	 * 提交归档任务
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
}
