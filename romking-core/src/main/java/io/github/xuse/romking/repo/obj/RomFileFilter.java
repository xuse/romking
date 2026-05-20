package io.github.xuse.romking.repo.obj;

import java.util.List;

import com.github.xuse.querydsl.annotation.query.Condition;
import com.github.xuse.querydsl.annotation.query.ConditionBean;
import com.querydsl.core.types.Ops;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.romking.core.GameType;
import io.github.xuse.romking.core.Platform;
import lombok.Data;

@Data
@ConditionBean
public class RomFileFilter {
	@Condition(Ops.EQ)
	private Integer dirId;

	@FormField(caption = "游戏名", placeHolder = "游戏名", type = InputType.TEXT)
	@Condition(Ops.STRING_CONTAINS)
	private String name;

	@FormField(caption = "ROM文件名", placeHolder = "ROM文件名", type = InputType.TEXT)
	@Condition(Ops.STRING_CONTAINS)
	private String romName;

	@Condition(Ops.EQ)
	private String gameid;

	@FormField(caption = "平台", type = InputType.COMBO)
	@Condition(Ops.EQ)
	private Platform platform;

	@Condition(Ops.IN)
	private List<GameType> gameType;

}
