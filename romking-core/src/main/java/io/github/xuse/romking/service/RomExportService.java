package io.github.xuse.romking.service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import com.github.xuse.querydsl.util.Assert;

import io.github.xuse.romking.metadata.ee.GameListService;
import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.enums.RepoType;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.tasks.ExportRomTask;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;

@Service
public class RomExportService {

	@Inject
	private GlobalTaskService taskService;

	@Inject
	private RomDirRepository romDirRepo;

	@Inject
	private RomFileRepository romFileRepo;

	@Inject
	private MediaFileRepository mediaRepo;

	@Inject
	private GameListService gameListService;

	/**
	 * 提交导出任务
	 *
	 * @param options 导出选项
	 */
	public void export(RomExportOptions options) {
		// 校验
		Assert.notNull(options.getTargetPath(), "目标路径不能为空");
		Assert.isTrue(options.getSourceDirIds() != null && options.getSourceDirIds().length > 0, "请选择源目录");

		File targetDir = new File(options.getTargetPath());
		Assert.isTrue(targetDir.exists() || targetDir.mkdirs(), "目标路径无法创建: " + options.getTargetPath());

		// 校验源目录必须是ARCHIVE类型
		for (int dirId : options.getSourceDirIds()) {
			RomDir dir = romDirRepo.load(dirId);
			Assert.notNull(dir, "源目录不存在: " + dirId);
			Assert.isTrue(dir.getType() == RepoType.ARCHIVE,
					"源目录必须是归档仓库类型: " + dir.getRootpath());
		}

		ExportRomTask task = new ExportRomTask(options, romDirRepo, romFileRepo, mediaRepo, gameListService);
		taskService.submit(task);
	}

	/**
	 * 获取所有ARCHIVE类型的目录列表（供UI选择源）
	 */
	public List<RomDir> listArchiveDirs() {
		return romDirRepo.find(q -> q.where(RomDirRepository.t.type.eq(RepoType.ARCHIVE)));
	}

	/**
	 * 获取指定仓库标签下的所有目录ID
	 */
	public int[] getDirIdsByLabel(String label) {
		List<RomDir> dirs = romDirRepo.find(q -> q.where(RomDirRepository.t.label.eq(label)));
		return dirs.stream().mapToInt(RomDir::getId).toArray();
	}
}
