package io.github.xuse.romking.metadata.ee;

public enum RomState {
	/**
	 * 尚未检查文件
	 */
	INIT,
	/**
	 * ROM文件缺失
	 */
	MISS_FILE,
	/**
	 * ROM文件存在 
	 */
	FILE_EXIST,
}
