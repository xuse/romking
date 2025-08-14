package io.github.xuse.romking.repo.enums;

import com.github.xuse.querydsl.types.CodeEnum;

public enum MediaType implements CodeEnum<MediaType>{
	IMAGE(1),
	VIDEO(2),
	SOUND(3),
	/**
	 * 通过BAT引用的本体
	 */
	DOS_ROM(10),
	OTHER(99)
	
	;
	public final int code;
	
	MediaType(int code) {
		this.code=code;
	}
	@Override
	public int getCode() {
		return code;
	}
}
