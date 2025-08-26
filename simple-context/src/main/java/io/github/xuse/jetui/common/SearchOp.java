package io.github.xuse.jetui.common;

/**
 * 每个字段的搜索条件
 * 
 * @author jiyi
 *
 */
public enum SearchOp {
	/**
	 * 等于
	 */
	EQ,
	/**
	 * 大于
	 */
	GT,
	/**
	 * 大于等于
	 */
	GE,
	/**
	 * 小于
	 */
	LT,
	/**
	 * 小于等于
	 */
	LE,
	/**
	 * like 'key%'
	 */
	MATCH_START,
	/**
	 * like '%key'
	 */
	MATCH_END,
	/**
	 * like '%key%'
	 */
	CONTAINS,
	/**
	 * between min-value and max-value
	 */
	BETWEEN,
	/**
	 * 无需搜索
	 */
	NONE
}
