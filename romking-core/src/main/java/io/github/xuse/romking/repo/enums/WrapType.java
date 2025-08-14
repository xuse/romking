package io.github.xuse.romking.repo.enums;

import com.github.xuse.querydsl.types.CodeEnum;

public enum WrapType implements CodeEnum<WrapType>{
	/**
	 * 文件即ROM本身
	 */
	ROM(0),
	/**
	 * 文件是目录，下有多个文件
	 */
	DIRECTORY(1),
	/**
	 * 文件是ZIP，单文件ROM在内。
	 */
	ZIPPED_ROM(2),
	/**
	 * 文件是ZIP，包含多个文件
	 */
	ZIPPED_DIRECTORY(3),
	;
	
	WrapType(int code){
		this.code=code;
	}
	
	public final int code;

	@Override
	public int getCode() {
		return code;
	}
}
