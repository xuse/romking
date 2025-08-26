package io.github.xuse.jetui.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 为了支持重复注解的容器
 * @author jiyi
 *
 */
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface ViewColumns {
	ViewColumn[] value() default {};
}
