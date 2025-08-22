package io.github.xuse.jetui.convert;

import java.lang.reflect.InvocationTargetException;

import org.springframework.core.convert.converter.Converter;

/**
 * getter方法执行转换器
 * @author jiyi
 *
 */
final class MethodAccessor implements Converter<Object, Object> {
	private java.lang.reflect.Method method;

	public MethodAccessor(java.lang.reflect.Method method) {
		this.method = method;
	}

	@Override
	public Object convert(Object source) {
		try {
			return method.invoke(source);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
}
