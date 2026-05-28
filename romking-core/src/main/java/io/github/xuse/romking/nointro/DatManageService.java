package io.github.xuse.romking.nointro;

import java.io.File;
import java.util.List;
import java.util.Set;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.dal.KnownRomRepository;
import io.github.xuse.romking.repo.obj.KnownRom;
import io.github.xuse.romking.service.GlobalTaskService;
import io.github.xuse.romking.tasks.ImportDatTask;
import io.github.xuse.romking.tasks.Task;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;

/**
 * DAT数据管理服务。
 * 提供DAT导入、ROM识别等功能。
 */
@Service
public class DatManageService {

	@Inject
	private GlobalTaskService taskService;

	@Inject
	private DatImportService importService;

	@Inject
	private KnownRomRepository knownRomRepo;

	/**
	 * 提交DAT导入任务
	 * 
	 * @param path DAT文件或目录路径
	 * @param platform 平台（null自动推断）
	 * @param isParentClone 是否为P/C格式
	 * @param platforms 要导入的平台集合（null表示全部）
	 * @return 提交的任务实例，可用于订阅进度通知
	 */
	public Task submitImport(String path, Platform platform, boolean isParentClone, Set<Platform> platforms) {
		File file = new File(path);
		if (!file.exists()) {
			throw new IllegalArgumentException("路径不存在: " + path);
		}
		ImportDatTask task = new ImportDatTask(file, platform, isParentClone, platforms, importService);
		taskService.submit(task);
		return task;
	}

	/**
	 * 提交DAT导入任务（兼容旧接口）
	 */
	public Task submitImport(String path, Platform platform, boolean isParentClone, boolean filterByConfig) {
		Set<Platform> platforms = filterByConfig ? DatImportConfig.DEFAULT_PLATFORMS : null;
		return submitImport(path, platform, isParentClone, platforms);
	}

	/**
	 * 提交DAT导入任务（默认按配置过滤）
	 */
	public Task submitImport(String path, Platform platform, boolean isParentClone) {
		return submitImport(path, platform, isParentClone, true);
	}

	/**
	 * 通过MD5查找已知ROM
	 */
	public KnownRom findByMd5(String md5) {
		if (md5 == null || md5.isEmpty()) return null;
		List<KnownRom> list = knownRomRepo.find(q ->
				q.where(KnownRomRepository.md5.eq(md5.toLowerCase())));
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * 通过CRC+大小查找已知ROM
	 */
	public List<KnownRom> findByCrcAndSize(String crc, long size) {
		if (crc == null || crc.isEmpty()) return List.of();
		return knownRomRepo.find(q ->
				q.where(KnownRomRepository.crc.eq(crc.toLowerCase()),
						KnownRomRepository.romSize.eq(size)));
	}

	/**
	 * 查找同一游戏家族的所有版本（通过parentName）
	 */
	public List<KnownRom> findFamily(String parentName) {
		if (parentName == null || parentName.isEmpty()) return List.of();
		return knownRomRepo.find(q ->
				q.where(KnownRomRepository.parentName.eq(parentName)));
	}

	/**
	 * 获取已导入的总条目数
	 */
	public long getKnownRomCount() {
		return knownRomRepo.getFactory().selectFrom(KnownRomRepository.t).fetchCount();
	}
}
