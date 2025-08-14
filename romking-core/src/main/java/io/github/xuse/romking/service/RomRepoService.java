package io.github.xuse.romking.service;

import java.io.File;

import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;

@Service
public class RomRepoService {
	@Inject
	private RomFileRepository repo;
	
	
	
	
	
	
	public void scan(File parent,RomScanOptions options) {
		
	}
}
