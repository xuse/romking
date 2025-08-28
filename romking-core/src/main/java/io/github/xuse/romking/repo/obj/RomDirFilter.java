package io.github.xuse.romking.repo.obj;

import com.github.xuse.querydsl.annotation.query.Condition;
import com.github.xuse.querydsl.annotation.query.ConditionBean;
import com.querydsl.core.types.Ops;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.enums.RepoType;
import lombok.Data;

@Data
@ConditionBean
public class RomDirFilter {
	@FormField(caption = "仓库标识")
	@Condition(Ops.STRING_CONTAINS)
	private String label;
	
	@FormField(caption = "目录")
	@Condition(Ops.EQ)
	private String rootpath;
	
	@FormField(caption = "标签")
	@Condition(Ops.STRING_CONTAINS)
	private String tags;
	
	@FormField(caption = "类型",type = InputType.COMBO)
	@Condition(Ops.EQ)
	private RepoType type;
	
	@FormField(caption = "平台",type = InputType.RADIO,selectItems = "enum://实例:instance,仓库:repo")
	@Condition(Ops.EQ)
	private Platform platform;
	
}
