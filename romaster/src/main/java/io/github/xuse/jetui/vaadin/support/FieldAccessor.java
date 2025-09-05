package io.github.xuse.jetui.vaadin.support;

import java.lang.reflect.Field;

import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;

import io.github.xuse.jetui.annotation.ViewColumn;
import lombok.SneakyThrows;

public class FieldAccessor<T> implements ValueProvider<T,Object>, Setter<T,Object>{
	private final Field field;
	private final ViewColumn column;
	
	public FieldAccessor(Field field, ViewColumn c) {
		super();
		this.field = field;
		this.column=c;
	}

	@SneakyThrows
	@Override
	public Object apply(T source) {
		Object v=field.get(source);
		boolean isNull = v==null;
		if(!isNull && column!=null && column.emptyStrignAsNull()) {
			isNull= "".equals(v);
		}
		return isNull?column==null?"":column.nullString():v;
	}
	
	@SneakyThrows
	public  void accept(T bean, Object fieldvalue) {
		field.set(bean, fieldvalue);
	}
	
}
