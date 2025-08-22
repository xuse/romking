package io.github.xuse.jetui.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.xuse.jetui.common.Enabled;
import io.github.xuse.jetui.common.InputType;

/**
 * 注解，用来表示前端动态表单界面上的一个编辑控件。
 * 
 * @author jiyi
 *
 */
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface FormField {
	public static final String allowDecimals = "allowDecimals";
	public static final String allowNegative = "allowNegative";
	public static final String minValue = "minValue";
	public static final String maxValue = "maxValue";
	public static final String unUseChars = "unUseChars";
	/**
	 * @return 列名
	 */
	String caption() default "";

	/**
	 * @return 输入框类型
	 */
	InputType type() default InputType.TEXT;

	/**
	 * @return 是否必填
	 */
	boolean required() default false;

	/**
	 * @return 校验类型
	 */
	String vtype() default "";

	/**
	 * @return 输入最大长度
	 */
	int maxLength() default 0;

	/**
	 * @return 缺省值
	 */
	String defaultValue() default "";

	/**
	 * @return 校验用表达式。当vtype=regexp时使用正则校验
	 */
	String regexp() default "";

	/**
	 * @return 校验失败后的提示语
	 */
	String errTip() default "";

	/**
	 * 显示的提示文字
	 */
	String hint() default "";

	/**
	 * @return 顺序
	 */
	int order() default 0;

	/**
	 * @return 样式宽度
	 */
	int witdh() default 0;

	/**
	 * @return 样式高度
	 */
	int height() default 0;

	/**
	 * 如果是combo/checkbox/radio，出现在选择中的选择项数据源<br/>
	 * 
	 * 
	 * 用以下格式表示:
	 * <ol>
	 * <li><strong>固定常量</strong>
	 * {@code enum://innodb:InnoDB;myisam:MyISAM;maria:MariaDB}<br/>
	 * 表述有三个选择项，显示值是InnoDB , MyISAM， MariaDB，对应的值是innodb, myisam, maria。</li>
	 * <li><strong>配置文件</strong>
	 * propertie://dsadfs.pro</li>
	 * <li><strong>SQL查询</strong>
	 * sql://select id as code,name as text from sdffd where lang=:user_lang<br>
	 * SQL协议，输入自定义的SQL语句查询，要求返回的两个字段分别为code,text,数字20表示缓存生存时长。包含查询变量</li>
	 * <li><strong>标准数据字典</strong>
	 * datadict://combo-name <br>
	 * 使用数据字典</li>
	 * <li><strong>调用Bean获得Map</strong>
	 * call://beanName.methodName(paramName) 调用一个SpringBean的方法。方法要求返回Map<String,String>类型
	 * </li>
	 * </ol>
	 * <p>
	 * <h3>修饰器</h3>
	 * 在:和//符号中间可以增加修饰器： 修饰器是一个JSON字符串。
	 * <pre>{
	 * cache: 20,
	 * add: {0:'平台内置',100:'其他'}, 
	 * addBefore:{ALL:'全部'}
	 * withCode:false, resort:false, forceResort:true}
	 * </pre>
	 * 其中——
	 * <ol>
	 * <li>cache 		确定数据库缓存的生存时间，对非数据库操作无效</li>
	 * <li>add   		在所有选项之后添加额外的选项</li>
	 * <li>addBefore 	在所有选项之前添加额外的选项</li>
	 * <li>withCode  	在选项文本前加上[code]文字</li>
	 * <li>resort    	对选项按code重新排序，add和addBefore添加项不参与排序</li>
	 * <li>forceResort	对选项按code重新排序，add和addBefore添加项也参与排序</li>
	 * </ol>
	 */
	String selectItems() default "";

	/**
	 * @return 日期控件时使用，描述日期格式
	 */
	String format() default "";


	/**
	 * @return 其他自定义属性，由控件来控制如何渲染，格式形如 {@code width:100px; height:200px}
	 */
	String customAttr() default "";

	/**
	 * 其他HTML属性，直接作为HTML属性出现在页面上。<br>
	 * 格式形如 {@code width:100px; height:200px}
	 * @return 其他HTML属性，
	 */
	String attribute() default "";
	
	/**
	 * 其他自定义样式，直接作为HTML中的Style属性出现在页面上。<br>
	 * @return 格式形如 {@code width:100px; height:200px}
	 */
	String style() default "";

	/**
	 * @return 该字段是否可用
	 */
	Enabled enabled() default Enabled.ALLWAYS;

	/**
	 * @return 如果为false，则不出现在编辑框中
	 */
	boolean edit() default true;
}
