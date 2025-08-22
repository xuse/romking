package io.github.xuse.romking.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.xuse.querydsl.util.Assert;

import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.vo.RomRepo;
import io.github.xuse.simple.context.InitializingBean;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;

@Service
public class RomMngService implements InitializingBean{
	@Inject
	private GlobalTaskService taskService;
	
	@Inject
	private RomDirRepository romDirRepo;
	
	@Inject
	private RomFileRepository romFileRepo;
	
	@Inject
	private MediaFileRepository mediaRepo;

	public List<RomRepo> listRepos(int limit, int offset, Optional<Void> filter) {
		List<RomRepo> list=romDirRepo.findRepos(limit, offset);
		for(RomRepo r:list) {
			List<RomDir> dirs=romDirRepo.find((s)->s.where(RomDirRepository.t.label.eq(r.getLabel())));
			List<Integer> dirIds=dirs.stream().map(RomDir::getId).collect(Collectors.toList());
			
			int romCount=romFileRepo.count((s)->s.where(RomFileRepository.dirId.in(dirIds)));
			r.setRoms(romCount);
			
			int mediaCount=mediaRepo.count((s)->s.where(MediaFileRepository.dirId.in(dirIds)));
			r.setMedias(mediaCount);
		}
		return list;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(taskService);
		Assert.notNull(romDirRepo);
		Assert.notNull(romFileRepo);
		Assert.notNull(mediaRepo);
	}
}
