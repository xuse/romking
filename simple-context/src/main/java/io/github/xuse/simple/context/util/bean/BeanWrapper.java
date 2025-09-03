package io.github.xuse.simple.context.util.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.github.xuse.querydsl.sql.expression.Property;

public interface BeanWrapper<T> {

	Property[] getProperties();
	
	String[] getRwPropertyNames();

	Type getPropertyGenericType(String name);
	
	Class<?> getPropertyType(String name);
	
	Object getPropertyValue(String name);

	void setPropertyValue(String name, Object value);

	String getClassName();

	<A extends Annotation> A getAnnotationOnField(String name, Class<A> type);

	T refresh();
	
	public static <T> BeanWrapper<T> creator(Class<T> beanClass) {
		return new BeanWrapperImpl<>(beanClass);
	}
	
	public static <T> BeanWrapper<T> of(T obj) {
		return new BeanWrapperImpl<>(obj);
	}

	void setPropertyValueByString(String keyName, String value);
}
