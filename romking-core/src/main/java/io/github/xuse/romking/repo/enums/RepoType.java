package io.github.xuse.romking.repo.enums;

import com.github.xuse.querydsl.types.CodeEnum;

public enum RepoType implements CodeEnum<RepoType>{
	/**
	 * 实例仓
	 */
	INSTANCE(0),
	/**
	 * 归档仓
	 */
	ARCHIVE(1)
	
	;
	public final int code;
	
	RepoType(int code) {
		this.code=code;
	}
	@Override
	public int getCode() {
		return code;
	}
}
