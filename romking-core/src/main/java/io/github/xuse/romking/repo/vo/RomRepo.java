package io.github.xuse.romking.repo.vo;

import lombok.Data;

@Data
public class RomRepo {
	/**
	 * 标签
	 */
	private String label;
	/**
	 * 目录
	 */
	private int dirs;
	/**
	 * rom文件
	 */
	private int roms;
	/**
	 * 媒体文件
	 */
	private int medias;

}
