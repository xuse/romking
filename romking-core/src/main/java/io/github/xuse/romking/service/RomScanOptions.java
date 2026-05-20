package io.github.xuse.romking.service;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.enums.RepoType;
import lombok.Data;

/**
 * ROM扫描选项
 */
@Data
public class RomScanOptions {

	/**
	 * 仓库标签（对应TF卡或ROM集合的名称）
	 */
	private String label;

	/**
	 * 指定平台（如果为null则尝试从目录名推断）
	 */
	private Platform platform;

	/**
	 * 仓库类型
	 */
	private RepoType repoType = RepoType.INSTANCE;

	/**
	 * 是否扫描没有gamelist.xml的目录
	 */
	private boolean scanWithoutGamelist = true;

	/**
	 * 是否计算MD5（耗时操作）
	 */
	private boolean computeMd5 = true;

	/**
	 * 是否计算CRC（对ZIP内文件，CRC可从ZIP头直接获取）
	 */
	private boolean computeCrc = true;
}
