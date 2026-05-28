package io.github.xuse.romking.service;

import com.github.xuse.querydsl.util.Assert;

import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.tasks.VerifyRepoTask;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;

/**
 * ROM仓库校验服务
 */
@Service
public class RomVerifyService {

	@Inject
	private GlobalTaskService taskService;

	@Inject
	private RomDirRepository romDirRepo;

	@Inject
	private RomFileRepository romFileRepo;

	/**
	 * 提交校验任务
	 *
	 * @param dirId 要校验的目录ID
	 * @param skipPreCheck 是否跳过预检（用户已确认继续时为true）
	 * @param quickMode 快速模式：ZIP用CRC校验（从头直接读取），非ZIP仅检查存在性
	 */
	public void verify(int dirId, boolean skipPreCheck, boolean quickMode) {
		RomDir dir = romDirRepo.load(dirId);
		Assert.notNull(dir, "目录不存在: " + dirId);

		VerifyRepoTask task = new VerifyRepoTask(dirId, romDirRepo, romFileRepo, skipPreCheck, quickMode);
		taskService.submit(task);
	}

	/**
	 * 提交完整校验任务（默认不跳过预检）
	 */
	public void verify(int dirId, boolean skipPreCheck) {
		verify(dirId, skipPreCheck, false);
	}
}
