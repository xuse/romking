package io.github.xuse.romking.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.github.xuse.querydsl.init.csv.Codecs;
import com.github.xuse.querydsl.sql.expression.BeanCodec;
import com.github.xuse.querydsl.sql.expression.BeanCodecManager;

public class BeanReader<T> implements RecordSet{
	private final Field[] fields;
	private final Object[] values;
	private int offset=-1;
	
	public BeanReader(T bean){
		BeanCodec codec=BeanCodecManager.getInstance().getCodec(bean.getClass());
		this.fields=codec.getFields();
		this.values=codec.values(bean);
	}
	
	public String getValueString() {
		return Codecs.toString(values[offset],fields[offset].getGenericType());
	}
	
	public Class<?> getValueType(){
		return fields[offset].getType();
	}
	
	public Type getValueGenericType(){
		return fields[offset].getGenericType();
	}
	
	public <X extends Annotation> X getAnnotation(Class<X> clz){
		return fields[offset].getAnnotation(clz);
	}
	
	public Object getValue() {
		return values[offset];
	}
	@Override
	public boolean next() {
		return ++offset<values.length;
	}

	public String getPropertyName() {
		return fields[offset].getName();
	}
}