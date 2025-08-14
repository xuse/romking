package io.github.xuse.romking.repo.obj;

import java.util.List;

import com.github.xuse.querydsl.annotation.query.Condition;
import com.github.xuse.querydsl.annotation.query.ConditionBean;
import com.querydsl.core.types.Ops;

import io.github.xuse.romking.core.GameType;
import io.github.xuse.romking.core.Platform;
import lombok.Data;

@Data
@ConditionBean
public class RomFileFilter {
	@Condition(Ops.EQ)
	private int dirId;
	
	@Condition(Ops.STRING_CONTAINS)
	private String name;
	
	@Condition(Ops.STRING_CONTAINS)
	private String romName;
	
	@Condition(Ops.EQ)
	private String gameid;
	
	@Condition(Ops.EQ)
	private Platform platform;
	
	@Condition(Ops.IN)
	private List<GameType> gameType;
	
}
