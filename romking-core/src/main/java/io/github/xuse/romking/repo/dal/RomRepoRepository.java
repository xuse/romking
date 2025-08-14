package io.github.xuse.romking.repo.dal;

import com.github.xuse.querydsl.repository.GenericRepository;
import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.RelationalPath;

import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.simple.context.Service;

@Service
public class RomRepoRepository extends GenericRepository<RomDir, Integer>{
	public SQLQueryFactory getFactory() {
		return factory;
	}
	
	public RelationalPath<RomDir> t() {
		return super.getPath();
	}
}
