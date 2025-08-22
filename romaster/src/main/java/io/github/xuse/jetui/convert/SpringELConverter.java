package io.github.xuse.jetui.convert;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * SpringEL的转换器
 * @author jiyi
 *
 */
public class SpringELConverter extends AbstractConverter<Object, String>{
	/**
	 * 表达式解析器
	 */
	private final static ExpressionParser parser = new SpelExpressionParser();
	
	/**
	 * 表达式文本
	 */
	private String text;
	
	
	/**
	 * 默认的全局上下文
	 */
	private StandardEvaluationContext context;
	
	/**
	 * 解析后的表达式
	 */
	private Expression spel;
	
	public SpringELConverter(FastJSONBeanConvertFilter parent,String exp) {
		super(parent);
		this.text=exp;
		this.context = new StandardEvaluationContext();
		try {
			this.spel=parser.parseExpression(exp);
		}catch(SpelParseException ex) {
			throw new IllegalArgumentException("Spring表达式错误:"+ text,ex);
		}
	}
	
	@Override
	public String toString() {
		return text;
	}

	@Override
	public String convert(Object source) {
		return spel.getValue(context,source,String.class);
	}

}
