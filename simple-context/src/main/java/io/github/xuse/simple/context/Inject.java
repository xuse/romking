package io.github.xuse.simple.context;

public @interface Inject {
	String name() default "";
	Class<?> type() default Object.class; 
}
