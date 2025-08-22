package io.github.xuse.jetui.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @View 多重注解的解决方案
 * 用于描述该模型在动态表单的界面上的视图显示效果
 * 
 * @author jiyi
 *
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface Views {
	
	View[] value() default {};
}
