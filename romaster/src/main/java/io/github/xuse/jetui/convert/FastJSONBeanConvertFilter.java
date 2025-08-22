package io.github.xuse.jetui.convert;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.converter.SmartHttpMessageConverter;

import com.alibaba.fastjson.serializer.AfterFilter;

import io.github.xuse.SpringContextUtil;

/**
 * 针对fastJSON转换完成后的数据进行后处理。
 * 根据一个表达式语言，进行后处理、
 * @author jiyi
 *
 */
public class FastJSONBeanConvertFilter extends AfterFilter {
	private SmartHttpMessageConverter parent;
	private final Map<String, Context> converts = new HashMap<String, Context>();
	private Class<?> clz;

	static final class Context {
		Converter<Object, String> converter;
		String name;
	}

	public FastJSONBeanConvertFilter(Class<?> clz,SmartHttpMessageConverter parent) {
		this.parent=parent;
		this.clz = clz;
	}

	public void addConvert(AnnotatedElement element, String converter, String name) {
		Context c = new Context();
		if(converter==null) {
			c.converter = new CompsiteConverter<>(getAccessor(element),ToStringConverter.INSTANCE);
		} else if (converter.startsWith("#{")) {
			int len = converter.length();
			c.converter = new SpringELConverter(this,converter.substring(2, len - 1));
		} else if (converter.startsWith("${")) {
			int len = converter.length();
			c.converter = new CompsiteConverter<>(getAccessor(element), new SpringELConverter(this,converter.substring(2, len - 1)));
		} else if (converter.startsWith("mask://")) {
			//FIXME 需要解决被隐藏的数据传送到前台的问题
			c.converter = new CompsiteConverter<>(getAccessor(element), new CommonMaskConverter(this,converter.substring(7)));
		} else {
			//枚举解析
			c.converter = getEnumsConverter(element,converter);
		}
		c.name = name;
		converts.put(name, c);
	}

	private Converter<Object,String> getEnumsConverter(AnnotatedElement element,String converter) {
		SelectMapResolver services = SpringContextUtil.getBean(SelectMapResolver.class);
		OptionResolver resolver=services.parse(converter);
		if(resolver.isConditional()) {
			//如果是和传入参数条件相关的
			return new BeanConditionalConvert(this,element,resolver);
		}else {
			//和传入参数无关的
			return new CompsiteConverter<>(getAccessor(element), new MapValueConverter(resolver));
		}
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

	@Override
	public void writeAfter(Object object) {
		if (object.getClass() == clz) {
			for (Context context : converts.values()) {
				super.writeKeyValue(context.name, context.converter.convert(object));
			}	
		}
	}
	
//	public <T> T getParam(Class<T> clz) {
//		return (T)parent.getParam(clz);
//	}
}
