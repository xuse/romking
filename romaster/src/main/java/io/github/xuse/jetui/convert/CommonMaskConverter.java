/* $Id: CommonMaskStrategy.java 219322 2012-10-18 12:38:37Z shiym $  */
package io.github.xuse.jetui.convert;

import com.github.xuse.querydsl.util.Assert;

/**
 * 通用模糊策略类
 * <p>
 * 实现一些较为常用的模糊策略，如替换前几位字符为*、末几位字符为*、将第几位之后、末几位之前的所有字符替换为*等。
 * </p>
 */
public class CommonMaskConverter extends AbstractConverter<Object, String> {
	/**
	 * 默认替换所有字符为"*"
	 */
	private static final String DEFAULT_MASK_EXPRESSION = "*(,)";

	/**
	 * 默认替换符为"*"
	 */
	private static final char DEFAULT_MASK_CHAR = '*';

	private Config parsedExpressions;
	
	static final class Config{
		private char replace;
		//如果为0，从第一个字符开始。
		private int start;
		
		private int len;
	}
	
	
	public static void main(String[] args) {
		String str="12345678";
		Assert.equals("********", new CommonMaskConverter(null,"(,)").mask(str));
		Assert.equals("########", new CommonMaskConverter(null,"#(,)").mask(str));
		Assert.equals("###45678", new CommonMaskConverter(null,"#(,3)").mask(str));
		Assert.equals("1*******", new CommonMaskConverter(null,"(1,)").mask(str));
		Assert.equals("12######", new CommonMaskConverter(null,"#(2,)").mask(str));
		
		
		Assert.equals("12###678", new CommonMaskConverter(null,"#(2,3)").mask(str));
		Assert.equals("1##45678", new CommonMaskConverter(null,"#(3,-2)").mask(str));
		Assert.equals("##345678", new CommonMaskConverter(null,"#(2,-2)").mask(str));
		Assert.equals("12#45678", new CommonMaskConverter(null,"#(-5,-1)").mask(str));
		Assert.equals("123##678", new CommonMaskConverter(null,"#(-5,2)").mask(str));
		Assert.equals("123456##", new CommonMaskConverter(null,"#(-2,5)").mask(str));
		Assert.equals("1#####78", new CommonMaskConverter(null,"#(-2,-5)").mask(str));
		
		Assert.equals("1234567#", new CommonMaskConverter(null,"#(-1,)").mask(str));
		Assert.equals("#######8", new CommonMaskConverter(null,"#(-1,-)").mask(str));
		Assert.equals("123#####", new CommonMaskConverter(null,"#(-5,)").mask(str));
		Assert.equals("###45678", new CommonMaskConverter(null,"#(-5,-)").mask(str));
		Assert.equals("#2345678", new CommonMaskConverter(null,"#(-7,-)").mask(str));
		Assert.equals("12345678", new CommonMaskConverter(null,"#(-8,-)").mask(str));
		System.out.println("Done");
	}

	/**
	 * 使用默认替换表达式，即替换所有字符为"*"
	 * @param　parent
	 */
	public CommonMaskConverter(FastJSONBeanConvertFilter parent) {
		super(parent);
		parsedExpressions = parse(DEFAULT_MASK_EXPRESSION);
	}

	/**
	 * 改造为基于JsonPath进行替换。
	 * 
	 * 
	 * @param expression <br>
	 *            表达式格式为：替换符fixed(leftOffset, rightOffset)<br>
	 *            替换符仅支持*和#, <br>
	 *            替换符, fixed, leftOffset, rightOffset 均可以不指定，但位置需保留；<br>
	 *            leftOffset 支持负数，负数表示距离末尾的偏移位置(即替换末几位字符)，负数时rightOffset值无效，<br>
	 *            leftOffset 不指定则默认为0; <br>
	 *            rightOffset 支持负数，负数表示距离末尾的偏移位置。<br>
	 *            <h3>举例：12345678 被替换后 </h3>
	 *            "(,)", 表示将所有字符替换为*<br>
	 *            "#(,)", 表示将所有字符替换为# ########<br>
	 *            "#(,3)", 表示将首3个字符替换为#  ###45678 <br>
	 *            "(1,)", 表示将1开始所有字符替换为*,  1******<br>
	 *            "#(2,)", 表示从第3个字符开始全部替换为#  12###### <br>
	 *            "#(2,3)", 表示从第3个字符开始，替换8个字符   12###678 <br>
	 *            "#(3,-2)", 表示从第3个字符开始，向前替换2个字符   1##45678 <br>
	 *            
	 *            //当起点为负数时，表示起点是从尾部计算的序号。而长度依然是从左向右。长度为负数时，即可表示从右向左。当长度不指定时，即长度为无穷大。如果要指定从右向左的无穷大，用单个负号
	 *            "#(-1,)", 表示将末1位之后的所有字符替换为#  1234567#<br>
	 *            "#(-5,)", 表示将末5位之后的所有字符替换     123#####<br>
	 *            "#(-1,-)", 表示将末1位之前的所有字符替换为# #######8<br>
	 *            "#(-5,-)", 表示将末5位之前的所有字符替换    ###45678<br>
	 *            "#(-5,-1)",表示将末5位之前的1个字符替换为# 12#45678<br> 
	 *            "#(-5,2)", 表示将末5位之后的2字符替换      123##678
	 *           
	 *            @param parent
	 */
	public CommonMaskConverter(FastJSONBeanConvertFilter parent,String expression) {
		super(parent);
		parsedExpressions = parse(expression);
		if (parsedExpressions == null) {
			throw new IllegalArgumentException("Illeagal mask expression:" + expression);
		}
	}

	private Config parse(String expr) {
		int left=expr.indexOf('(');
		int right=expr.indexOf(')');
		String s=expr.substring(0,left);
		Config c=new Config();
		c.replace=s.length()==0?DEFAULT_MASK_CHAR:s.charAt(0);
		
		int comma=expr.indexOf(',');
		String n1 = expr.substring(left + 1, comma).trim();
		String n2 = expr.substring(comma + 1, right).trim();
		
		c.start = n1.isEmpty() ? 0 : Integer.parseInt(n1);
		
		c.len = n2.isEmpty() ? Integer.MAX_VALUE : "-".equals(n2) ? -Integer.MAX_VALUE : Integer.parseInt(n2);
		return c;
	}

	private String mask(String srcValue) {
		Config c=this.parsedExpressions;
		int srcLength=srcValue.length();
		StringBuilder sb=new StringBuilder(srcLength);
		if(c.start>=0) {
			if(c.len>=0) {
				sb.append(srcValue.subSequence(0, c.start));
				int len=Math.min(srcLength-c.start, c.len);
				repeat(sb,c.replace,len);
				sb.append(srcValue.substring(c.start+len));
			}else {
				//计算替换字符数
				int len=Math.min(c.start, -c.len);
				int begin=c.start-len;
				sb.append(srcValue.subSequence(0, begin));
				repeat(sb,c.replace,len);
				sb.append(srcValue.substring(c.start));
			}
		}else{
			if(c.len>=0) {
				int len = Math.min(-c.start, c.len);
				sb.append(srcValue.subSequence(0, srcLength+c.start));
				repeat(sb, c.replace, len);
				sb.append(srcValue.substring(srcLength + c.start + len ));
			}else {
				int len = Math.min(srcLength + c.start, -c.len);
				sb.append(srcValue.subSequence(0, srcLength + c.start - len));
				repeat(sb, c.replace, len);
				sb.append(srcValue.substring(srcLength + c.start));
			}
		}
		return sb.toString();
	}


	private void repeat(StringBuilder sb, char c,int len) {
		for(int i=0;i<len;i++)
			sb.append(c);
	}

	@Override
	public String convert(Object source) {
		if (source == null)
			return null;
		String value = String.valueOf(source);
		return mask(value);
	}
}
