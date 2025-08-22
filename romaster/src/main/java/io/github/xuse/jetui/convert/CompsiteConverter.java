package io.github.xuse.jetui.convert;

import org.springframework.core.convert.converter.Converter;


/**
 * 复合的转换器 
 * @author jiyi
 *
 * @param <S>
 * @param <T>
 * @param <X>
 */
public class CompsiteConverter<S, T, X> implements Converter<S, T> {
	private Converter<S, X> c1;
	private Converter<X, T> c2;

	public CompsiteConverter(Converter<S, X> a1, Converter<X, T> a2) {
		this.c1 = a1;
		this.c2 = a2;
	}

	@Override
	public T convert(S source) {
		return c2.convert(c1.convert(source));
	}
}
