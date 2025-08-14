package io.github.xuse.romking.tasks;

import com.github.xuse.querydsl.types.CodeEnum;

public enum TaskType implements CodeEnum<TaskType>{
	SCAN_DIR(false,1)
	;
	TaskType(boolean singleton, int code){
		this.singleton=singleton;
		this.code=code;
	}
	
	public final boolean singleton;
	
	public final int code;

	@Override
	public int getCode() {
		return code;
	}
}
