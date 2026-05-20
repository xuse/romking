package io.github.xuse.romaster.ui.archive;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.common.InputType;
import lombok.Data;

/**
 * 归档操作表单
 */
@Data
public class ArchiveForm {

	@FormField(caption = "源目录ID(INSTANCE)", placeHolder = "源目录ID", type = InputType.NUMBER)
	private int sourceDirId;

	@FormField(caption = "目标目录ID(ARCHIVE)", placeHolder = "目标目录ID", type = InputType.NUMBER)
	private int targetDirId;
}
