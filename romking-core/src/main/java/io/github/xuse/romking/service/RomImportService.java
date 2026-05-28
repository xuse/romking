package io.github.xuse.romking.service;

import java.io.File;

import io.github.xuse.romking.metadata.ee.GameListService;
import io.github.xuse.romking.nointro.DatManageService;
import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.tasks.ScanRomTask;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;

@Service
public class RomImportService {
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

	@Inject
	private DatManageService datManageService;

	/**
	 * 提交扫描任务
	 * 
	 * @param dir     要扫描的根目录
	 * @param options 扫描选项
	 */
	public void scan(File dir, RomScanOptions options) {
		ScanRomTask task = new ScanRomTask(dir, options,
				romDirRepo, romFileRepo, mediaRepo, gameListService, datManageService);
		taskService.submit(task);
	}
}
