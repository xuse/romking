package io.github.xuse.romking.tasks;

import com.github.xuse.querydsl.types.CodeEnum;

public enum TaskType implements CodeEnum<TaskType>{
	SCAN_DIR(false,1),
	EXPORT(false,2),
	ARCHIVE(false,3),
	VERIFY(false,4),
	IMPORT_DAT(false,5)
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
