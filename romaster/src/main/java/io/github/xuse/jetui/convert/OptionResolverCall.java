package io.github.xuse.jetui.convert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import com.github.xuse.querydsl.util.StringUtils;

import io.github.xuse.SpringContextUtil;
import io.github.xuse.jetui.common.BaseException;


/**
 * 枚举选项解析器——反射调用实现
 * @author jiyi
 *
 */
public class OptionResolverCall implements OptionResolver {
	private Object service;
	private Method method;
	private String[] paramNames;
	private static Logger logger = LoggerFactory.getLogger(OptionResolverCall.class);
	
	/**
	 * 从已解析的常量构造
	 * 
	 * @param values
	 */
	public OptionResolverCall(String call) {
		int index=call.indexOf(".");
		if(index==-1) {
			throw new IllegalArgumentException("指定SpringBean不存在 call://"+call);
		}
		String beanName=call.substring(0,index);
		this.service=SpringContextUtil.getBean(beanName);
		
		int methodStart= call.indexOf('(',index);
		String methodName;
		String params;
		if(methodStart==-1) {
			methodName=call.substring(index+1);
			params="";
		}else {
			methodName=call.substring(index+1,methodStart);
			params=call.substring(methodStart+1,call.length()-1);
		}
		this.method=BeanUtils.findMethodWithMinimalParameters(service.getClass(), methodName);
		if(method==null) {
			throw new IllegalArgumentException("指定方法不存在 call://"+call+",目标类型为"+service.getClass().getName());
		}
		this.paramNames=StringUtils.split(params,",");
		if(method.getParameterTypes().length!=paramNames.length) {
			throw new IllegalArgumentException("指定方法的参数不匹配 call://"+call+",目标类型为"+method);
		}
		Class<?> rt=method.getReturnType();
		if(rt!=Map.class) {
			throw new IllegalArgumentException("指定方法的返回类型不匹配 call://"+call+",要求返回Map<String,String>类型");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getValues(Map<String, Object> params) {
		Object[] p=fixParams(params);
		Object result;
		try {
			result = method.invoke(service, p);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new BaseException(e);
		} catch (InvocationTargetException e) {
			logger.error("Error while calling OptionResolver",e);
			result=Collections.singletonMap("", "服务器端错误");
		}
		return (Map<String,String>)result;
	}
	

	/**
	 * 用户传入的参数列表中可能有大量无关参数，此处需要过滤掉无关参数，仅保留SQL相关的几项参数进行操作。
	 * @param params
	 * @return
	 */
	private Object[] fixParams(Map<String, Object> params) {
		if(paramNames.length==0) {
			return new Object[0];
		}
		Object[] result=new Object[paramNames.length];
		for(int i=0;i<paramNames.length;i++) {
			result[i]=params.get(paramNames[i]);
		}
		return result;
	}
	
	
	
	@Override
	public boolean isConstants() {
		return false;
	}

	@Override
	public boolean isConditional() {
		return paramNames.length>0;
	}

}
