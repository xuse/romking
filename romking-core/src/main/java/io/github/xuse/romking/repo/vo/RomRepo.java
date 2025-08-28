package io.github.xuse.romking.repo.vo;

import io.github.xuse.jetui.annotation.ViewColumn;
import lombok.Data;

@Data
public class RomRepo {
	/**
	 * 标签
	 */
	@ViewColumn(caption = "仓库名称")
	private String label;
	/**
	 * 目录
	 */
	@ViewColumn(caption = "目录数")
	private int dirs;
	/**
	 * rom文件
	 */
	@ViewColumn(caption = "ROM数")
	private int roms;
	/**
	 * 媒体文件
	 */
	@ViewColumn(caption = "文件数")
	private int medias;
}
