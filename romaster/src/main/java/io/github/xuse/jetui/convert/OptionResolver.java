package io.github.xuse.jetui.convert;

import java.util.Map;

/**
 * 枚举值解析器
 * 
 * @author jiyi
 *
 */
public interface OptionResolver {
	
	/**
	 * 根据传入的参数返回解析的结果
	 * @param params
	 * @return
	 */
	Map<String, String> getValues(Map<String, Object> params);
	
	
	/**
	 * 如果解析结果是固定常量的，返回true
	 * @return
	 */
	boolean isConstants();
	
	/**
	 * 解析结果是取决于传入参数的，返回true
	 * @return
	 */
	boolean isConditional();
}
