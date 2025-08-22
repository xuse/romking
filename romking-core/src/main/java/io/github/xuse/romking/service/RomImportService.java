package io.github.xuse.romking.service;

import java.io.File;

import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
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
	
	
	
	
	
	
	public void scan(File parent,RomScanOptions options) {
		
	}
}
