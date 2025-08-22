package io.github.xuse.jetui.common;

/**
 * 描述一个字段在页面上是否处于可编辑状态。
 * @author jiyi
 *
 */
public enum Enabled {
	/**
	 * 总是启用
	 */
	ALLWAYS,
	/**
	 * 总是Disabled
	 */
	DISABLED,
	/**
	 * 总是Readonly
	 */
	READONLY,
	/**
	 * 当编辑时DISABLED
	 */
	DISABLED_FOR_EDIT,
	/**
	 * 当编辑时READONLY
	 */
	READONLY_FOR_EDIT
}
