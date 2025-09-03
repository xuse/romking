package io.github.xuse.romking.manager.obj;

public enum EmuType {
	nes(Company.nintendo),
	snes(Company.nintendo),
	
	
	;
	public final Company company;
	private EmuType(Company company) {
		this.company=company;
	}
}
