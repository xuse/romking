package io.github.xuse.romking.core;

public enum Platform {
	
	NES(Company.NINTENDO,true,""),
	SNES(Company.NINTENDO,true,""),
	N64(Company.NINTENDO,true,""),
	NGC(Company.NINTENDO,true,""),
	Wii(Company.NINTENDO,true,""),
	Switch(Company.NINTENDO,true,""),
	VirtualBoy(Company.NINTENDO,true,""),
	GB(Company.NINTENDO,true,""),
	GBC(Company.NINTENDO,true,""),
	GBA(Company.NINTENDO,true,""),
	NDS(Company.NINTENDO,true,""),
	n3DS(Company.NINTENDO,true,""),
	
	MD(Company.SEGA,true,""),
	//MD-CD(Company.SEGA,true),
	SS(Company.SEGA,true,""),
	DC(Company.SEGA,true,""),
	
	PS(Company.SONY,true,""),
	PS2(Company.SONY,true,""),
	
	PSP(Company.SONY,true,""),
	PSV(Company.SONY,false,""),
	
	PORTS(Company.OTHER,false,""),
	DOS(Company.OTHER,false,""),
	;
	
	private Platform(Company company,boolean singleFile,String iconUrl) {
		this.company=company;
		this.singleFile=singleFile;
		this.iconUrl=iconUrl;
	}
	
	private String[] exts;
	public final boolean supportZip = true;
	public final boolean singleFile;
	public final Company company;
	public final String iconUrl;

}
