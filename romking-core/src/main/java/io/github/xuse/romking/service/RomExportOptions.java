package io.github.xuse.romking.service;

import io.github.xuse.romking.metadata.ParserType;
import lombok.Data;

/**
 * ROM导出选项
 */
@Data
public class RomExportOptions {

	/**
	 * 源仓库ID列表（ARCHIVE仓库中选择的目录ID）
	 */
	private int[] sourceDirIds;

	/**
	 * 目标TF卡根路径
	 */
	private String targetPath;

	/**
	 * 目标仓库标签
	 */
	private String targetLabel;

	/**
	 * 导出模式：true=覆盖（删除目标中多余的ROM），false=增量（保留目标现有ROM）
	 */
	private boolean overwrite = false;

	/**
	 * 快速导出：开启后如果目标卡上有相同游戏文件，不校验MD5，直接按成功处理
	 */
	private boolean quickExport = false;

	/**
	 * 导出的游戏列表格式
	 */
	private ParserType metadataFormat = ParserType.EMUELEC;
}
