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
	 * 是否计算MD5（耗时操作，大文件平台建议关闭，归档时再补算）
	 */
	private boolean computeMd5 = true;

	/**
	 * 是否计算CRC（对ZIP内文件，CRC可从ZIP头直接获取，开销极低）
	 */
	private boolean computeCrc = true;

	/**
	 * 增量扫描模式：如果数据库中已有该文件记录，且 size+lastModified 未变，则跳过不重新计算hash。
	 * 适用于重新扫描已有仓库的场景。
	 */
	private boolean incremental = false;
}
