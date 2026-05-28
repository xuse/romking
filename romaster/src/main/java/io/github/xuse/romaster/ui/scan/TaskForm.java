package io.github.xuse.romaster.ui.scan;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.romking.core.Platform;
import lombok.Data;

@Data
public class TaskForm {
	@FormField(caption = "仓库标签", placeHolder = "仓库标签", type = InputType.TEXT)
	private String label;

	@FormField(caption = "扫描路径", placeHolder = "扫描路径", type = InputType.TEXT)
	private String path;

	@FormField(caption = "平台", type = InputType.COMBO)
	private Platform platform;

	@FormField(caption = "增量扫描（跳过未变更文件）", type = InputType.CHECKBOX)
	private boolean incremental = false;

	@FormField(caption = "扫描无列表目录", type = InputType.CHECKBOX)
	private boolean scanWithoutGamelist = true;

	@FormField(caption = "计算MD5", type = InputType.CHECKBOX)
	private boolean computeMd5 = true;

	@FormField(caption = "计算CRC", type = InputType.CHECKBOX)
	private boolean computeCrc = true;
}
