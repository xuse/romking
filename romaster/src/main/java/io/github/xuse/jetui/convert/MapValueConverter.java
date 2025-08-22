package io.github.xuse.jetui.convert;

import org.springframework.core.convert.converter.Converter;

/**
 * 供视图使用的枚举映射转换器
 * @author jiyi
 *
 */
final class MapValueConverter implements Converter<Object, String> {
	private OptionResolver resolver;

	public MapValueConverter(OptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public String convert(Object value) {
		if(value==null)return null;
		String key = String.valueOf(value);
		String text = resolver.getValues(null).get(key);
		return text == null ? key : text;
	}
}
