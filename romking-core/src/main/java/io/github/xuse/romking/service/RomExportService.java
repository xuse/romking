package io.github.xuse.romking.service;

import java.io.File;

import com.github.xuse.querydsl.init.ScanOptions;

import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.tasks.ScanRomTask;
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
	
	
	
	/**
	 * 扫描
	 * @param dir
	 * @param options
	 * @param label
	 */
	public void scan(File dir,ScanOptions options,String label) {
		ScanRomTask task=new ScanRomTask(dir,label,options);
		taskService.submit(task);
	}

}
