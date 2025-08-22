package io.github.xuse.romking.repo.dal;

import java.util.List;
import java.util.stream.Collectors;

import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.querydsl.core.Tuple;
import com.querydsl.sql.RelationalPath;

import io.github.xuse.romking.repo.obj.QRomDir;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.obj.RomDirFilter;
import io.github.xuse.romking.repo.vo.RomRepo;
import io.github.xuse.simple.context.Service;

@Service
public class RomDirRepository extends AbstractRepository<RomDir, Integer,RomDirFilter>{
	public static final QRomDir t = QRomDir.romDir;
	
	
	public SQLQueryFactory getFactory() {
		return factory;
	}
	
	public RelationalPath<RomDir> t() {
		return super.getPath();
	}
	
	public int getRepoCount(){
		return factory.select(t.label.countDistinct()).from(t).fetchFirst().intValue();
	}
	
	public List<RomRepo> findRepos(int limit,int offset){
		List<Tuple> tuples=factory.select(t.label,t.id.count()).from(t).groupBy(t.label).limit(limit).offset(offset).fetch();
		return tuples.stream().map(this::toRepo).collect(Collectors.toList());
	}
	
	private RomRepo toRepo(Tuple tuple) {
		RomRepo repo=new RomRepo();
		repo.setLabel(tuple.get(t.label));
		repo.setDirs(tuple.get(1, Integer.class));
		return repo;
	}
}
