package io.github.xuse.jetui.convert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.mysema.commons.lang.Pair;

/**
 * 枚举修饰器
 * @author jiyi
 *
 */
public class DecoratorResolver implements OptionResolver{
	private Decorator dec;
	private OptionResolver or;
	
	public DecoratorResolver(Decorator dec, OptionResolver optionResolver) {
		this.or=optionResolver;
		this.dec=dec;
	}

	@Override
	public Map<String, String> getValues(Map<String, Object> params) {
		Map<String,String> raw=or.getValues(params);
		if(dec.resort && !dec.forceResort) {
			raw=new LinkedHashMap<>(new TreeMap<String,String>(raw));
		}
		if(dec.forceResort) {
			//因为还要重新排序，所以直接添加到Map即可。
			raw=addAfter(raw,dec.getAddBefore());
			raw=addAfter(raw,dec.getAddAfter());
			raw=new TreeMap<String,String>(raw);
		}else {
			raw=addBefore(raw,dec.getAddBefore());
			raw=addAfter(raw,dec.getAddAfter());
		}
		if(dec.isWithCode()) { 
			for(Map.Entry<String, String> entry: raw.entrySet()) {
				String value="["+entry.getKey()+"]"+entry.getValue();
				entry.setValue(value);
			}
		}
		return raw;
	}

	private Map<String, String> addBefore(Map<String, String> raw, List<Pair<String,String>> add) {
		if(add.isEmpty())return raw;
		Map<String,String> result=new LinkedHashMap<String,String>();
		for(Pair<String,String> ss:add) {
			result.put(ss.getFirst(), ss.getSecond());
		}
		result.putAll(raw);
		return result;
	}

	private Map<String, String> addAfter(Map<String, String> raw, List<Pair<String,String>> add) {
		if(!add.isEmpty()) {
			if(raw.isEmpty()){
				raw=new LinkedHashMap<String,String>(add.size());
			}
			for(Pair<String,String> ss:add) {
				raw.put(ss.getFirst(), ss.getSecond());
			}
		}
		return raw;
	}

	@Override
	public boolean isConstants() {
		return or.isConstants();
	}

	@Override
	public boolean isConditional() {
		return or.isConditional();
	}
	
	
	public static class Decorator {
		/**
		 * 文本带有Code
		 */
		private boolean withCode;
		/**
		 * 缓存秒数
		 */
		private int cacheSeconds = 20;
		/**
		 * 重排序
		 */
		private boolean resort;
		/**
		 * 强行重排序
		 */
		private boolean forceResort;
		/**
		 * 添加在前
		 */
		private List<Pair<String,String>> addBefore;
		/**
		 * 添加项
		 */
		private List<Pair<String,String>> addAfter;
		
		
		public boolean isResort() {
			return resort;
		}
		public void setResort(boolean resort) {
			this.resort = resort;
		}
		public boolean isForceResort() {
			return forceResort;
		}
		public void setForceResort(boolean forceResort) {
			this.forceResort = forceResort;
		}
		public boolean isWithCode() {
			return withCode;
		}
		public void setWithCode(boolean withCode) {
			this.withCode = withCode;
		}
		public int getCacheSeconds() {
			return cacheSeconds;
		}
		public void setCacheSeconds(int cacheSeconds) {
			this.cacheSeconds = cacheSeconds;
		}
		public List<Pair<String,String>> getAddBefore() {
			if(addBefore==null) return Collections.emptyList();
			return addBefore;
		}
		public void setAddBefore(List<Pair<String,String>> addBefore) {
			this.addBefore = addBefore;
		}
		public List<Pair<String,String>> getAddAfter() {
			if(addAfter==null)return Collections.emptyList();
			return addAfter;
		}
		public void setAddAfter(List<Pair<String,String>> addAfter) {
			this.addAfter = addAfter;
		}
	}
}
