package io.github.xuse.jetui.convert;

import java.net.URL;
import java.util.Map;

import com.github.xuse.querydsl.util.IOUtils;

/**
 * 基于Properties实现的枚举解析
 * @author jiyi
 *
 */
public class OptionResolverPropsImpl implements OptionResolver {
	private URL resource;
	
	private Map<String,String> values;
	

	public OptionResolverPropsImpl(String substring) {
		resource = this.getClass().getClassLoader().getResource(substring);
		if (resource != null) {
			values=IOUtils.loadProperties(resource);
		} else {
			throw new IllegalArgumentException("The resource file " + substring + " is not exist!");
		}
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
