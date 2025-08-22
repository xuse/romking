package io.github.xuse.romking.repo.dal;

import java.util.Optional;
import java.util.stream.Stream;

import com.github.xuse.querydsl.repository.GenericRepository;

public class AbstractRepository<T,ID,F> extends GenericRepository<T, ID>{
	public int count(Optional<F> f) {
		if(f.isPresent()) {
			return countByCondition(f.get());	
		}else {
			return (int)factory.selectFrom(getPath()).fetchCount();
		}
	}
	
	public Stream<T> list(Optional<F> f,int offset,int limit){
		if(f.isPresent()) {
			return findByCondition(f.get(), limit, offset).getSecond().stream();
		}else {
			return factory.selectFrom(getPath()).offsetIf(offset).limitIf(limit).fetch().stream();
		}
	}

}
