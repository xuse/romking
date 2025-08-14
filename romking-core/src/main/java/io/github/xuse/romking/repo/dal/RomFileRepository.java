package io.github.xuse.romking.repo.dal;

import com.github.xuse.querydsl.repository.GenericRepository;
import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.RelationalPath;

import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.simple.context.Service;

@Service
public class RomFileRepository extends GenericRepository<RomFile, Integer>{
	public SQLQueryFactory getFactory() {
		return factory;
	}
	public RelationalPath<RomFile> t() {
		return super.getPath();
	}
}
