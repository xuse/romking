package io.github.xuse.jetui.common;

@FunctionalInterface
public interface Form {
	Object getValue(String field);
	
	default String getString(String field) {
		Object v= getValue(field);
		return v==null?null:v.toString();
	}
	
	default int getInt(String field, int defaultValue) {
		Object v= getValue(field);
		if(v instanceof Number) {
			return ((Number) v).intValue();
		}
		return defaultValue;
	}

	default long getLong(String field, long defaultValue) {
		Object v= getValue(field);
		if(v instanceof Number) {
			return ((Number) v).longValue();
		}
		return defaultValue;
	}
	
	default double getDouble(String field, double defaultValue) {
		Object v= getValue(field);
		if(v instanceof Number) {
			return ((Number) v).doubleValue();
		}
		return defaultValue;
	}
	
	default boolean getBoolean(String field, boolean defaultValue) {
		Object v= getValue(field);
		if(v instanceof Boolean) {
			return ((Boolean) v).booleanValue();
		}
		if(v instanceof String) {
			if("0".equals(v) || "F".equals(v)) {
				return false;
			}
			if("1".equals(v)||"T".equals(v)) {
				return true;
			}
		}
		return defaultValue;
	}
}

