package io.github.xuse.jetui.common;

/**
 * 描述调用服务时的一个参数
 * 
 * @author jiyi
 * 
 */
public class Param {
	/**
	 * 参数的数据类型
	 */
	Class<?> type;

	/**
	 * 参数的功能说明
	 */
	String desc;

	public Param() {
	}

	public Param(Class<?> type, String desc) {
		this.type = type;
		this.desc = desc;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
