package io.github.xuse.simple.context.util.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.xuse.querydsl.init.csv.Codecs;
import com.github.xuse.querydsl.sql.expression.BeanCodec;
import com.github.xuse.querydsl.sql.expression.BeanCodecManager;
import com.github.xuse.querydsl.sql.expression.Property;
import com.github.xuse.querydsl.util.ArrayUtils;
import com.github.xuse.querydsl.util.Exceptions;

public class BeanWrapperImpl<T> implements BeanWrapper<T>{
	final BeanCodec codec;
	
	T bean;
	
	final Object[] values;
	
	final String[] names;
	
	
	
	
	BeanWrapperImpl(Class<T> beanClass) {
		this.codec=BeanCodecManager.getInstance().getCodec(beanClass);
		this.values=new Object[codec.getFields().length];
		this.names=Arrays.stream(codec.getFields()).map(Property::getName).collect(Collectors.toList()).toArray(ArrayUtils.EMPTY_STRING_ARRAY);
	}
	
	public BeanWrapperImpl(T bean) {
		this.bean = bean;
		this.codec = BeanCodecManager.getInstance().getCodec(bean.getClass());
		this.values = codec.values(bean);
		this.names=Arrays.stream(codec.getFields()).map(Property::getName).collect(Collectors.toList()).toArray(ArrayUtils.EMPTY_STRING_ARRAY);
	}

	@Override
	public String[] getRwPropertyNames() {
		return names;
	}

	@Override
	public Type getPropertyGenericType(String name) {
		Integer index=codec.getRandomAccessIndex().get(name);
		if(index==null) {
			throw Exceptions.noSuchElement("There's no property {} in class {}", name, codec.getType().getName());
		}
		return codec.getFields()[index].getGenericType();
	}
	

	@Override
	public Class<?> getPropertyType(String name) {
		Integer index=codec.getRandomAccessIndex().get(name);
		if(index==null) {
			throw Exceptions.noSuchElement("There's no property {} in class {}", name, codec.getType().getName());
		}
		return codec.getFields()[index].getType();
	}

	@Override
	public void setPropertyValueByString(String name, String value) {
		Integer index = codec.getRandomAccessIndex().get(name);
		if (index == null) {
			throw Exceptions.noSuchElement("There's no property {} in class {}", name, codec.getType().getName());
		}
		values[index] = Codecs.fromString(value, codec.getFields()[index].getGenericType());
	}
	
	
	@Override
	public void setPropertyValue(String name, Object value) {
		Integer index=codec.getRandomAccessIndex().get(name);
		if(index==null) {
			throw Exceptions.noSuchElement("There's no property {} in class {}", name, codec.getType().getName());
		}
		values[index]=value;
	}

	@Override
	public String getClassName() {
		return codec.getType().getName();
	}

	@Override
	public <A extends Annotation> A getAnnotationOnField(String name, Class<A> type) {
		Integer index=codec.getRandomAccessIndex().get(name);
		if(index==null) {
			throw Exceptions.noSuchElement("There's no property {} in class {}", name, codec.getType().getName());
		}
		return codec.getFields()[index].getAnnotation(type);
	}
	
	@SuppressWarnings("unchecked")
	public T refresh() {
		if(bean==null) {
			return this.bean = (T)codec.newInstance(values);
		}else {
			codec.sets(values, bean);
			return bean;
		}
	}

	@Override
	public Object getPropertyValue(String name) {
		Integer index=codec.getRandomAccessIndex().get(name);
		if(index==null) {
			throw Exceptions.noSuchElement("There's no property {} in class {}", name, codec.getType().getName());
		}
		return values[index];
	}

	@Override
	public Property[] getProperties() {
		return codec.getFields();
	}
}
