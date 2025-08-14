package io.github.xuse.romking.repo.obj;

import com.github.xuse.querydsl.annotation.query.Condition;
import com.github.xuse.querydsl.annotation.query.ConditionBean;
import com.querydsl.core.types.Ops;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.enums.RepoType;
import lombok.Data;

@Data
@ConditionBean
public class RomDirFilter {
	@Condition(Ops.EQ)
	private String label;
	
	@Condition(Ops.EQ)
	private String rootpath;
	
	@Condition(Ops.STRING_CONTAINS)
	private String tags;
	
	@Condition(Ops.EQ)
	private RepoType type;
	
	@Condition(Ops.EQ)
	private Platform platform;
	
}
