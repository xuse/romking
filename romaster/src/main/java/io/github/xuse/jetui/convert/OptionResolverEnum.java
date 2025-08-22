package io.github.xuse.jetui.convert;

import java.util.Map;

import io.github.xuse.jetui.JetUIs;

public class OptionResolverEnum implements OptionResolver {
	private Map<String, String> values;

	/**
	 * 从已解析的常量构造
	 * 
	 * @param values
	 */
	public OptionResolverEnum(Map<String, String> values) {
		this.values = values;
	}

	/**
	 * 从URL配置构造
	 * 
	 * @param substring
	 * @param params
	 */
	public OptionResolverEnum(String substring) {
		this.values = JetUIs.toMap(substring, ";", ":", 0);
	}

	@Override
	public Map<String, String> getValues(Map<String, Object> params) {
		return values;
	}

	@Override
	public boolean isConstants() {
		return true;
	}

	@Override
	public boolean isConditional() {
		return false;
	}

}
