package io.github.xuse.jetui.convert;

import org.springframework.core.convert.converter.Converter;

/**
 * 转换器
 * @author jiyi
 *
 */
final class ToStringConverter implements Converter<Object, String> {
	public static final ToStringConverter INSTANCE=new ToStringConverter();
	
	private ToStringConverter() {
	}
	
	
	@Override
	public String convert(Object source) {
		return String.valueOf(source);
	}
}
