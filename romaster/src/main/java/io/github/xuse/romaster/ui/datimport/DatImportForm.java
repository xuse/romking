package io.github.xuse.romaster.ui.datimport;

import java.util.Set;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.nointro.DatImportConfig;
import lombok.Data;

/**
 * DAT导入表单
 */
@Data
public class DatImportForm {

	@FormField(caption = "DAT文件目录", placeHolder = "DAT/ZIP文件所在目录路径", type = InputType.TEXT)
	private String datPath;

	/**
	 * 要导入的平台集合。
	 * 不通过AutoForm绑定，由DatImportView手动处理多选组件。
	 */
	private Set<Platform> platforms = DatImportConfig.DEFAULT_PLATFORMS;
}
