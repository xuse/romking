package io.github.xuse.simple.context;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.xuse.querydsl.asm.ASMUtils;
import com.github.xuse.querydsl.asm.ASMUtils.ClassAnnotationExtracter;
import com.github.xuse.querydsl.asm.ClassReader;
import com.github.xuse.querydsl.spring.core.resource.Resource;
import com.github.xuse.querydsl.util.Assert;
import com.github.xuse.querydsl.util.ClassScanner;
import com.github.xuse.querydsl.util.Exceptions;
import com.github.xuse.querydsl.util.TypeUtils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 实现仿Spring BeanFactory的最轻量实现，自己开发小应用时使用
 */
@Slf4j
public class ApplicationContext {
	private final Map<Class<?>,List<Object>> typeContext;
	private final Map<String,Object>  nameContext;
	

	private ApplicationContext(Map<Class<?>,List<Object>> types,Map<String,Object> names) {
		this.typeContext=types;
		this.nameContext=names;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> clz) {
		Assert.notNull(clz,"The input class is null.");
		List<Object> list=typeContext.get(clz);
		if(list.size()==1) {
			return (T)list.get(0);
		}
		throw Exceptions.illegalArgument("There are {} beans with type {}", list.size(),clz.getName());
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getBean(String name) {
		Assert.notNull(name,"The input name is null.");
		return (T)nameContext.get(name);
	}
	
	public Iterable<Map.Entry<String,Object>> beans(){
		return new Iterable<Map.Entry<String,Object>>(){
			@Override
			public Iterator<Entry<String, Object>> iterator() {
				return nameContext.entrySet().iterator();
			}
		};
	}
	
	public static ContextBuilder builder() {
		return new ContextBuilder();
	}
	
	private static String uncapitalize(String str) {
		if(str==null || str.length()==0) {
			return str;
		}
		char c=str.charAt(0);
		if(Character.isUpperCase(c)) {
			char[] cs=str.toCharArray();
			cs[0]=Character.toLowerCase(c);
			return new String(cs); 
		}
		return str;
	}
	
	public static class ContextBuilder{
		private final Map<String,Object> nameContext=new HashMap<>();
		private final Map<Class<?>,List<Object>> typeContext=new HashMap<>();
		private final List<String> serviceClz=new ArrayList<>();
		private boolean processResourceAnnotation = false;
		
		
		public ContextBuilder processResourceAnnotation(boolean flag) {
			this.processResourceAnnotation=flag;
			return this;
		}
		
		public ContextBuilder addBean(Object bean) {
			String name=uncapitalize(bean.getClass().getSimpleName());
			return addBean(name,bean);
		}
		
		private void putType(Class<?> clz,Object bean) {
			List<Object> list=typeContext.computeIfAbsent(clz, (e)->new ArrayList<>());
			list.add(bean);
		}
		
		public ContextBuilder addBean(String name, Object bean) {
			Object old=this.nameContext.put(name, bean);
			if(old!=null) {
				throw new IllegalArgumentException("Duplicate bean name [" + name + "].");
			}
			putType(bean.getClass(), bean);
			return this;
		}
		public ApplicationContext build() {
			injectBeans();
			return new ApplicationContext(typeContext, nameContext);
		}
		
		@SneakyThrows
		private void injectBeans() {
			for(Object bean:nameContext.values()) {
				Class<?> type=bean.getClass();
				while(type!=Object.class) {
					for(Field field:type.getDeclaredFields()) {
						{
							Inject annotation=field.getAnnotation(Inject.class);
							if(annotation!=null) {
								injectField(bean,field,annotation);
							}	
						}
						if(processResourceAnnotation){
							javax.annotation.Resource annotation=field.getAnnotation(javax.annotation.Resource.class);
							if(annotation!=null) {
								log.info("Injecting @Resource field:{}.{}",field.getDeclaringClass(),field.getName());
								injectField(bean,field,annotation);
							}	
						}
					}
					type=type.getSuperclass();
				}
			}
			for(Object bean:nameContext.values()) {
				if(bean instanceof InitializingBean) {
					((InitializingBean) bean).afterPropertiesSet();
				}
			}
		}
		
		@SneakyThrows
		private void injectField(Object bean, Field field, javax.annotation.Resource anno) {
			Object member = anno.name().isEmpty()
					? first(typeContext.get(anno.type() == Object.class ? field.getType() : anno.type()))
					: nameContext.get(anno.name());
			if(member==null) {
				throw Exceptions.illegalArgument("Bean not found for field {}", field);
			}
			field.setAccessible(true);
			field.set(bean, member);
		}
		
		@SneakyThrows
		private void injectField(Object bean, Field field, Inject anno) {
			Object member = anno.name().isEmpty()
					? first(typeContext.get(anno.type() == Object.class ? field.getType() : anno.type()))
					: nameContext.get(anno.name());
			if(member==null) {
				throw Exceptions.illegalArgument("Bean not found for field {}", field);
			}
			field.setAccessible(true);
			field.set(bean, member);
		}
		
		@SneakyThrows
		public ContextBuilder scan(String packageName) {
			new ClassScanner().filterWith(this::hasAnnotation).scan(new String[]{packageName});
			ClassLoader loader=Thread.currentThread().getContextClassLoader();
			for(String clzName:serviceClz) {
				Class<?> clz=loader.loadClass(clzName);
				Service s=clz.getAnnotation(Service.class);
				String beanName=s.value().isBlank()?clzName:s.value().trim();
				Object bean=TypeUtils.newInstance(clz);
				log.info("[Adding Bean {}]:{}",beanName, bean);
				Object old=nameContext.put(beanName, bean);
				if(old!=null) {
					throw Exceptions.illegalArgument("There are duplicate name of beans,[{}] has {}\n{}", beanName, old, bean);
				}
				putType(clz, bean);
			}
			return this;
		}
		
		public Object first(List<?> list) {
			Assert.equals(1, list.size());
			return list.get(0);
		}
		@SneakyThrows
		private boolean hasAnnotation(Resource resource) {
			if (!resource.isReadable()) {
				return false;
			}
			ClassReader cl ;
			try(InputStream in=resource.getInputStream()){
				cl = new ClassReader(in);
			}
			ClassAnnotationExtracter extractor=new ClassAnnotationExtracter();
			cl.accept(extractor, ClassReader.SKIP_CODE);
			boolean flag= extractor.hasAnnotation(Service.class);
			if(flag) {
				serviceClz.add(ASMUtils.getJavaClassName(cl));
			}
			return flag;
		}
	}
	


}
