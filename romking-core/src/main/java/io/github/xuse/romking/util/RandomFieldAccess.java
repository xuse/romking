package io.github.xuse.romking.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.xuse.querydsl.init.csv.Codecs;
import com.github.xuse.querydsl.sql.expression.BeanCodec;
import com.github.xuse.querydsl.sql.expression.BeanCodecManager;
import com.github.xuse.querydsl.util.Assert;

/**
 * 在EF中提供了一个随机访问字段的机制，新版本后不再提供，为了兼容老代码并且综合性能，使用新版的替代反射机制实现。
 */
public class RandomFieldAccess {
	public static class Property{
		public final int order;
		public final Field field;
		public Property(int order,Field field) {
			this.order=order;
			this.field=field;
		}
		public String getName() {
			return field.getName();
		}
		public <T extends Annotation> T getAnnotation(Class<T> type) {
			return field.getAnnotation(type);
		}
	}
	private static final Map<Class<?>,Map<String,Property>> ORDERS=new ConcurrentHashMap<>();
	
	
	private static Map<String,Property> getOrder(BeanCodec type){
		return ORDERS.computeIfAbsent(type.getType(), (e)->generate(type));
	}
	
	private static Map<String,Property> generate(BeanCodec type) {
		Map<String,Property> order=new HashMap<>();
		Field[] fields=type.getFields();
		for(int i=0;i<fields.length;i++) {
			Field field=fields[i];
			order.put(field.getName(), new Property(i,field));
		}
		return order;
	}
	
	public static class BeanBuilder<T> extends RandomFieldAccess{
		private final BeanCodec codec;
		
		private final Object[] values;
		
		private final Map<String,Property> orders;
		
		
		public BeanBuilder(Class<T> clz) {
			this.codec=BeanCodecManager.getInstance().getCodec(clz);
			this.values=new Object[codec.getFields().length];
			this.orders=getOrder(codec);
		}
		
		@SuppressWarnings("unchecked")
		public T build() {
			return (T)codec.newInstance(values);
		}

		public Property[] getPropertyNames() {
			return orders.values().toArray(new Property[0]);
		}
		
		public void setPropertyValue(String key, Object v) {
			Property o=orders.get(key);
			Assert.notNull(o);
			values[o.order]=v;
		}

		public void setPropertyValueByString(Property o, String string) {
			Assert.notNull(o);
			values[o.order]=Codecs.fromString(string,o.field.getGenericType());
		}
	}
}
