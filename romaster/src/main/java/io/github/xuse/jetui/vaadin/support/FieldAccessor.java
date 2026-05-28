package io.github.xuse.jetui.vaadin.support;

import java.lang.reflect.Field;

import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;

import io.github.xuse.jetui.annotation.ViewColumn;
import lombok.SneakyThrows;

public class FieldAccessor<T> implements ValueProvider<T,Object>, Setter<T,Object>{
	private final Field field;
	private final ViewColumn column;
	private final String converter;
	
	public FieldAccessor(Field field, ViewColumn c) {
		super();
		field.setAccessible(true);
		this.field = field;
		this.column=c;
		this.converter = (c != null && !c.converter().isEmpty()) ? c.converter() : null;
	}

	@SneakyThrows
	@Override
	public Object apply(T source) {
		Object v=field.get(source);
		boolean isNull = v==null;
		if(!isNull && column!=null && column.emptyStrignAsNull()) {
			isNull= "".equals(v);
		}
		if (isNull) {
			return column == null ? "" : column.nullString();
		}
		if (converter != null) {
			return applyConverter(v);
		}
		return v;
	}

	private Object applyConverter(Object v) {
		switch (converter) {
		case "fileSize":
			if (v instanceof Number) {
				return FormatUtils.formatFileSize(((Number) v).longValue());
			}
			return v;
		default:
			return v;
		}
	}
	
	@SneakyThrows
	public  void accept(T bean, Object fieldvalue) {
		field.set(bean, fieldvalue);
	}
	
}
