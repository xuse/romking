package io.github.xuse.romking.repo.obj;

import com.github.xuse.querydsl.annotation.query.Condition;
import com.github.xuse.querydsl.annotation.query.ConditionBean;
import com.querydsl.core.types.Ops;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.enums.Region;
import lombok.Data;

@Data
@ConditionBean
public class KnownRomFilter {

	@FormField(caption = "平台", type = InputType.COMBO)
	@Condition(Ops.EQ)
	private Platform platform;

	@FormField(caption = "区域", type = InputType.COMBO)
	@Condition(Ops.EQ)
	private Region region;

	@FormField(caption = "游戏名", placeHolder = "输入游戏名关键字", type = InputType.TEXT)
	@Condition(Ops.STRING_CONTAINS)
	private String gameName;
}
