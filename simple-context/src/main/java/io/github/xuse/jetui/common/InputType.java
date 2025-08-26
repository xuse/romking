package io.github.xuse.jetui.common;

/**
 * 输入方式，以及数据类型
 * @author jiyi
 *
 */
public enum InputType {
	/**
	 * 文本输入框 - 单行
	 */
	TEXT,
	/**
	 * 文本输入框- 多行
	 */
	TEXTAREA,
	/**
	 * 密码输入框  - String
	 */
	PASSWORD,
	/**
	 * 下拉框 - String
	 */
	COMBO,
	
	/**
	 * 树形下拉框
	 */
	TREE_COMBO,
	
	/**
	 * 单选框组 - String
	 */
	RADIO,
	/**
	 * 复选框组 - String，多个项目用逗号分隔
	 */
	CHECKBOX,
	/**
	 * 整型数值，Integer
	 */
	NUMBER,
	/**
	 * 文件上传，对应类型File
	 */
	FILE,
	/**
	 * 一个DISABLED的输入框。高输入框的值仅供显示，不会被提交。
	 * @deprecated 不建议使用，使用Enabled来控制字段是否可编辑
	 */
	READONLY,
	/**
	 * 隐藏不可见的INPUT
	 */
	HIDDEN,
	/**
	 * 日期选择框
	 */
	DATE,
	/**
	 * Span文本
	 */
	LABEL,
}
