package io.github.xuse.simple.context.util.xml;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ FIELD })
@Retention(RUNTIME)
@Documented
public @interface XMLField {
	String value() default "";
	boolean serializeToAttr() default false;
	boolean writable() default true;

}
