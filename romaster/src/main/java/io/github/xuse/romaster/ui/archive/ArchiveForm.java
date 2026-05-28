package io.github.xuse.romaster.ui.archive;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.common.InputType;
import lombok.Data;

/**
 * 归档操作表单
 */
@Data
public class ArchiveForm {

	@FormField(caption = "源仓库(INSTANCE)", placeHolder = "选择源INSTANCE仓库label", type = InputType.TEXT)
	private String sourceLabel;

	@FormField(caption = "目标仓库(ARCHIVE)", placeHolder = "选择目标ARCHIVE仓库label", type = InputType.TEXT)
	private String targetLabel;
}
