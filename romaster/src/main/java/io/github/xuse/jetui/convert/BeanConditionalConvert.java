package io.github.xuse.jetui.convert;

import java.lang.reflect.AnnotatedElement;

import org.springframework.core.convert.converter.Converter;


/**
 * 根据Bean的数据来转换字典的实现
 * @author jiyi
 *
 */
public class BeanConditionalConvert  extends AbstractConverter<Object, String>{
	//访问待映射字段的方法
	private Converter<Object, Object> accessor;
	
	
	private OptionResolver resolver;

	/**
	 * 构造
	 * @param element 待映射字段
	 * @param resolver 映射规则解析器
	 */
	public BeanConditionalConvert(FastJSONBeanConvertFilter parent,AnnotatedElement element,OptionResolver resolver){
		super(parent);
		this.accessor=getAccessor(element);
		this.resolver=resolver;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String convert(Object source) {
		//提取待映射字段内容
		Object value=accessor.convert(source);
		if(value==null)return null;
		
		
		//将传入的bean整个转化为Map
//		JSONPolicy model=super.getParam(JSONPolicy.class);
//		Map<String,Object> params; 
//		if(model==null) {
//			params=Collections.EMPTY_MAP;
//		}else {
//			params=new HashMap<>();
//			params.put("scenario", model.scenario());
//		}
		
		//解析映射，并转化
//		Map<String,String> mappings=resolver.getValues(params);
		String key = String.valueOf(value);
//		String text = mappings.get(key);
//		return text == null ? key : text;
		return null;
	}
	
	private Converter<Object, Object> getAccessor(AnnotatedElement element) {
		Converter<Object, Object> accessor;
		if (element instanceof java.lang.reflect.Field) {
			accessor = new FieldAccessor((java.lang.reflect.Field) element);
		} else {
			accessor = new MethodAccessor((java.lang.reflect.Method) element);
		}
		return accessor;
	}
}
