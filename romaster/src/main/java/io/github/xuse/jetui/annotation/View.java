package io.github.xuse.jetui.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 用于描述该模型在动态表单的界面上的视图显示效果
 * 
 * @author jiyi
 *
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface View {
	/**
	 * @return 添加在视图上，用于描述查询优化参数
	 */
	boolean cascadeToOne() default true;

	/**
	 * @return 添加在视图上，用于描述查询优化参数
	 */
	boolean cascadeToMany() default false;
	
	/**
	 * @return 缺省排序是否倒序
	 */
	boolean defaultIsDesc() default false;
	
	/**
	 * @return 缺省排序字段
	 */
	String defaultOrderField() default "";
	
	/**
	 * 该注解支持的场景条件，空表示全部支持
	 */
	String[] scenario() default {};
	
	/**
	 * 该注解使用的视图控件。默认为空，由UI解析层自行渲染 
	 */
	String component() default "";
	
	/**
	 * 缺省查询参数
	 */
	String defaultParam() default "";
	
}
