package io.github.xuse.jetui.convert;

import org.springframework.core.convert.converter.Converter;

/**
 * 字段访问转换器
 * @author jiyi
 *
 */
final class FieldAccessor implements Converter<Object, Object>{
	private java.lang.reflect.Field field;

	/**
	 * 构造
	 * @param field
	 */
	public FieldAccessor(java.lang.reflect.Field field) {
		field.setAccessible(true);
		this.field=field;
	}
	
	@Override
	public Object convert(Object source) {
		try {
			return field.get(source);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
}
