package io.github.xuse.jetui.convert;

import org.springframework.core.convert.converter.Converter;

public abstract class AbstractConverter<F,T> implements Converter<F, T>{
	private FastJSONBeanConvertFilter parent;
	
	
	AbstractConverter(FastJSONBeanConvertFilter parent){
		this.parent=parent;
	}
	
//	protected <X> X getParam(Class<X> clz) {
//		return parent.getParam(clz);
//	}
}
