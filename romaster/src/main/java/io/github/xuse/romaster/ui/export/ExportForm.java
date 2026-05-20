package io.github.xuse.romaster.ui.export;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.romking.metadata.ParserType;
import lombok.Data;

@Data
public class ExportForm {

	@FormField(caption = "源仓库标签", type = InputType.TEXT)
	private String sourceLabel;

	@FormField(caption = "目标TF卡路径", type = InputType.TEXT)
	private String targetPath;

	@FormField(caption = "目标标签", type = InputType.TEXT)
	private String targetLabel;

	@FormField(caption = "覆盖模式", type = InputType.CHECKBOX)
	private boolean overwrite = false;

	@FormField(caption = "快速导出(跳过MD5校验)", type = InputType.CHECKBOX)
	private boolean quickExport = false;

	@FormField(caption = "元数据格式", type = InputType.COMBO)
	private ParserType metadataFormat = ParserType.EMUELEC;
}
