package io.github.xuse.romking.service;

import java.io.File;

import com.github.xuse.querydsl.init.ScanOptions;

import io.github.xuse.romking.tasks.ScanRomTask;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;

@Service
public class ScanService {
	
	@Inject
	private GlobalTaskService taskService;
	
	
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
