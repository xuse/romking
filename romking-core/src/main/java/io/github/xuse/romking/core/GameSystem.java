package io.github.xuse.romking.core;

public enum GameSystem {
	
	
	psx(Platform.PS),
	genesis(Platform.MD)
	;
	
	public final Platform platform;
	private GameSystem(Platform platform) {
		this.platform=platform;
	}
}
