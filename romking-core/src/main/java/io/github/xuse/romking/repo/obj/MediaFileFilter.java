package io.github.xuse.romking.repo.obj;

import com.github.xuse.querydsl.annotation.query.BoolCase;
import com.github.xuse.querydsl.annotation.query.Condition;
import com.github.xuse.querydsl.annotation.query.ConditionBean;
import com.github.xuse.querydsl.annotation.query.When;
import com.querydsl.core.types.Ops;

import io.github.xuse.romking.repo.enums.MediaType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConditionBean
public class MediaFileFilter {
	@Condition(Ops.EQ)
	private Integer dirId; 
	
	@When(forBool= {
		@BoolCase(is=true,ops=Ops.GT,value="0"),
		@BoolCase(is=false,ops=Ops.EQ,value="0")
	})
	private Boolean hasRefer;
	
	@Condition(Ops.EQ)
	private String filepath;
	
	@Condition(Ops.EQ)
	private MediaType type;

	@Condition(Ops.EQ)
	private String md5;
	
	@Condition(Ops.EQ)
	private String ext;
}
