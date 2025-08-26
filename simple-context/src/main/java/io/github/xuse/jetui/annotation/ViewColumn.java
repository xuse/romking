package io.github.xuse.jetui.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.xuse.jetui.common.SearchOp;

/**
 * 用于描述该模型在动态表单的界面上的视图显示效果
 * 
 * @author jiyi
 *
 */
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface ViewColumn {

	/**
	 * @return 列名
	 */
	String caption();
	
	String nullString() default "";
	
	boolean emptyStrignAsNull() default false;

	/**
	 * @return View中的宽度权重
	 */
	int width() default 10;

	/**
	 * @return View中的对齐方式
	 */
	String align() default "";

	/**
	 * TODO 函数直接写在代码中当如何
	 * 
	 * @return View中的渲染函数。配置函数名即可。函数可写在页面内。
	 */
	String renderer() default "";

	/**
	 * @return View中是否显示
	 */
	boolean visible() default true;

	/**
	 * 格式，仅对日期型字段有效
	 */
	String format() default "";
	
	/**
	 * @return 是否可排序
	 */
	boolean sortable() default false;
	
	/**
	 * @deprecated 重新考虑
	 * 是否在简易搜索中参与为查询条件
	 */
	boolean searchSimple() default false;
	
	/**
	 * @deprecated 重新考虑
	 * @return 在高级搜索中作为可输入的查询条件
	 */
	SearchOp search() default SearchOp.NONE;

	/**
	 * @return 序号，数值越小排越前
	 */
	int order() default 0;

	/**
	 * 显示在视图中的值转换器。支持以下语法
	 * 
	 * <h3>表达式计算</h3>
	 * <ul>
	 * 	<li>Spring表达式。格式为#{表达式}。以当前Bean作为根对象</li>
	 * 	<li>Spring表达式。格式为${表达式}。以当前字段值作为根对象</li>
	 * </ul>
	 *  关于Spring表达式，请自行百度Spring EL
	 *  
	 * <h3>马赛克</h3>
	 * <ul>
	 * <li>马赛克——mask://</li>
	 * </ul>
	 * 详见类{@link CommonMaskConverter#CommonMaskConverter()}的注解
	 * 马赛克用于屏蔽文字中的部分信息，减少敏感信息泄漏的可能，如身份证号等信息。<p>
	 * 
	 * <h3>枚举映射转换</h3>
	 * 详见{@link FormField#selectItems}
	 * <ul>
	 * <li>枚举映射——enum:// </li>
	 * <li>枚举映射——properties:// </li>
	 * <li>枚举映射——sql://</li>
	 * <li>枚举映射——dict://</li>
	 * <li>枚举映射——call://</li>
	 * </ul>
	 * 每种映射规则描述中还支持配置修饰器
	 * @return
	 * 
	 */
	String converter() default "";
	
	/**
	 * 某些时候。界面视图的列并不直接等同于数据库中的列。<br>
	 * 而当界面排序或者搜索时，需要知道数据库中对应的列名。<br>
	 * 默认当排序或搜索时，按照标注所在的列进行排序，某些时候不能确定数据库排序列的，可以在这里指定数据库列名。
	 * @return dbColumnName
	 */
	String dbColumnName() default "";
	
	
	/**
	 * 当前注解所属的场景（scenario）
	 * 一个对象模型可以在不同的视图中显示，此时不同的场景具有不同的scenario属性，这里配置该项生效的scenario。默认的scenario名为default。要支持多个scenario可以如下配置
	 * 比如 condition={"default","select"}
	 * 
	 * @return scenario
	 */
	String[] scenario() default {};
	
}
