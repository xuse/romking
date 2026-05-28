package io.github.xuse.romking.repo.vo;

import io.github.xuse.romking.core.Platform;
import lombok.Data;

/**
 * 重复ROM分组。
 * 表示一组gameid相同（或MD5相同）的ROM条目。
 */
@Data
public class DuplicateGroup {
	/**
	 * 分组标识（gameid 或 md5）
	 */
	private String groupKey;

	/**
	 * 游戏名（取组内第一条的displayName或name）
	 */
	private String gameName;

	/**
	 * 平台
	 */
	private Platform platform;

	/**
	 * 组内条目数
	 */
	private int count;

	/**
	 * 重复类型：SAME_GAME（同gameid不同版本）/ SAME_MD5（完全相同文件）
	 */
	private DuplicateType type;

	public enum DuplicateType {
		/** 同一游戏的不同版本（gameid相同，MD5不同） */
		SAME_GAME,
		/** 完全相同的文件（MD5相同） */
		SAME_MD5
	}
}
