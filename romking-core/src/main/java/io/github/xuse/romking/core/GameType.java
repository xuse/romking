package io.github.xuse.romking.core;

public enum GameType {
	RPG("角色扮演"),
	ARPG("动作角色扮演"),
	ACT("动作"),
	STG("射击"),
	FPS("第一人称射击"),
	RTS("实时策略"),
	SLG("策略"),
	AVG("冒险解谜"),
	SIM("真实模拟"),
	SRPG("战棋"),
	SPR("运动"),
	TAB("桌上"),
	ETC("其他")
	;
	public final String desc;
	GameType(String desc){
		this.desc=desc;
	}
}
