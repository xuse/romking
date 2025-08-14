package io.github.xuse.romking.repo.enums;

import com.github.xuse.querydsl.types.CodeEnum;

public enum Region implements CodeEnum<Region>{
	JPN(1),
	EUR(2),
	USA(3),
	ASA(10),
	//包括其他地区版本的中文汉化游戏，如果汉化过程完全未修改游戏内容，则不算HackROM。
	CHN(11),
	KOR(12),
	OTHER(99);
	public final int code;
	
	Region(int code) {
		this.code=code;
	}
	@Override
	public int getCode() {
		return code;
	}
	
}
