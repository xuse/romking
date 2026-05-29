package io.github.xuse.romking.repo.dal;

import java.util.List;

import com.github.xuse.querydsl.repository.GenericRepository;

import io.github.xuse.romking.repo.obj.ExportPreset;
import io.github.xuse.simple.context.Service;

/**
 * 导出预设仓库
 */
@Service
public class ExportPresetRepository extends GenericRepository<ExportPreset, Integer> {

	/**
	 * 获取所有预设列表
	 */
	public List<ExportPreset> listAll() {
		return find(q -> {});
	}
}
