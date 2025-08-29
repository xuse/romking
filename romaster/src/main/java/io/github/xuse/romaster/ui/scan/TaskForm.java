package io.github.xuse.romaster.ui.scan;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.romking.core.Platform;
import lombok.Data;

@Data
public class TaskForm {
	@FormField(type=InputType.TEXT)
	private String label;
	
	@FormField(type=InputType.TEXT)
	private String path;
	
	@FormField(type=InputType.COMBO)
	private Platform platform;
	
	
	

}
