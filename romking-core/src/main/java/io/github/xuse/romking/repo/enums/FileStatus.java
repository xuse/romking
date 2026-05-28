package io.github.xuse.romking.repo.enums;

import com.github.xuse.querydsl.types.CodeEnum;

/**
 * ROM文件状态
 */
public enum FileStatus implements CodeEnum<FileStatus> {
	/**
	 * 正常：文件存在且校验通过
	 */
	OK(0),
	/**
	 * 缺失：文件不存在
	 */
	MISSING(1),
	/**
	 * 损坏：文件存在但MD5不匹配
	 */
	CORRUPTED(2);

	public final int code;

	FileStatus(int code) {
		this.code = code;
	}

	@Override
	public int getCode() {
		return code;
	}
}
