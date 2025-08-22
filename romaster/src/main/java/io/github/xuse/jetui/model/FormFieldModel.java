package io.github.xuse.jetui.model;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONType;
import com.github.xuse.querydsl.util.StringUtils;

import io.github.xuse.SpringContextUtil;
import io.github.xuse.jetui.JetUIs;
import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.annotation.ViewColumn;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.jetui.convert.OptionResolver;
import io.github.xuse.jetui.convert.SelectMapResolver;
import lombok.Getter;
import lombok.Setter;

/**
 * 在界面上产生一个编辑控件。
 * 
 * @author jiyi
 *
 */
@Getter
@Setter
@JSONType(ignores = "order")
public class FormFieldModel implements Comparable<FormFieldModel> {
	private static final Map<String, String> SINGLE;
	static {
		SINGLE = new HashMap<String, String>();
		SINGLE.put("true", "");
	}

	public static FormFieldModel of(String name, AnnotatedElement element) {
		FormField annotation = element.getAnnotation(FormField.class);
		if (annotation == null) {
			if (element instanceof Method) {
				Method getter = (Method) element;
				String methodName = getter.getName();
				if (methodName.startsWith("get")) {
					methodName = methodName.substring(3);
				} else if (methodName.startsWith("is")) {
					methodName = methodName.substring(2);
				} else {
					throw new IllegalArgumentException("There's no Annotation @FormField on " + element);
				}
				try {
					Method setter = getter.getDeclaringClass().getMethod("set" + methodName, getter.getReturnType());
					annotation = setter.getAnnotation(FormField.class);
				} catch (NoSuchMethodException | SecurityException e) {
					throw new IllegalArgumentException(
							"There's no Annotation @FormField on " + element + " nor its setter.");
				}
			}
		}
		if (annotation == null) {
			throw new NullPointerException("There's no Annotation @FormField on " + element);
		}
		String text = annotation.caption();
		if (text.length() == 0) {
			ViewColumn c = element.getAnnotation(ViewColumn.class);
			if (c != null) {
				text = c.caption();
			}
		}
		FormFieldModel model = new FormFieldModel(name, text);
		model.setOrder(annotation.order());
		model.setRequired(annotation.required());
		model.setMaxLength(annotation.maxLength());
		model.setType(annotation.type());
		model.setRegexp(annotation.regexp());
		model.setErrTip(annotation.errTip());
		model.setHint(annotation.hint());
		if (annotation.witdh() > 0) {
			model.addStyle("width", annotation.witdh() + "px");
		}
		if (annotation.height() > 0) {
			model.addStyle("height", annotation.height() + "px");
		}
		if (annotation.format().length() > 0) {
			model.setFormat(annotation.format());
		} else if (annotation.type() == InputType.DATE) {
			//处理日期格式等问题
//				Temporal anno = element.getAnnotation(Temporal.class);
//				if (anno != null) {
//					switch (anno.value()) {
//					case DATE:
//						break;
//					case TIME:
//						model.setFormat("HH:mm:ss");
//						break;
//					case TIMESTAMP:
//						model.setFormat("yyyyMMdd HH:mm:ss");
//						break;
//					}
//				}
		}
		if (annotation.defaultValue().length() > 0) {
			model.setValue(annotation.defaultValue());
		}
		if (annotation.vtype().length() > 0) {
			model.setVtype(annotation.vtype());
		}
		if (annotation.attribute().length() > 0) {
			Map<String, String> map = JetUIs.toMap(annotation.attribute(), ";", ":", 0);
			for (Map.Entry<String, String> e : map.entrySet()) {
				model.addAttribute(e.getKey(), e.getValue());
			}
		}
		if (annotation.customAttr().length() > 0) {
			Map<String, String> map = JetUIs.toMap(annotation.customAttr(), ";", ":", 0);
			for (Map.Entry<String, String> e : map.entrySet()) {
				model.addCustomAttr(e.getKey(), e.getValue());
			}
		}
		if (annotation.style().length() > 0) {
			Map<String, String> map = JetUIs.toMap(annotation.style(), ";", ":", 0);
			for (Map.Entry<String, String> e : map.entrySet()) {
				model.addStyle(e.getKey(), e.getValue());
			}
		}
		if (StringUtils.isNotEmpty(annotation.selectItems())) {
			SelectMapResolver provider = SpringContextUtil.getBean(SelectMapResolver.class);
			OptionResolver resolver = provider.parse(annotation.selectItems());
			model.prepareSelectItmesMap(resolver);
		}
		switch (annotation.enabled()) {
		case ALLWAYS:
			break;
		case DISABLED:
			model.addAttribute("disabled", "true");
			break;
		case DISABLED_FOR_EDIT:
			model.addAttribute("noedit", "disabled");
			break;
		case READONLY:
			model.addAttribute("readonly", "true");
			break;
		case READONLY_FOR_EDIT:
			model.addAttribute("noedit", "readonly");
		default:
			break;
		}
		return model;
	}

	/**
	 * 字段名
	 */
	private String code;
	/**
	 * 显示名
	 */
	private String name;

	/**
	 * 默认值
	 */
	private Object value;
	/**
	 * 输入框
	 */
	private InputType type;

	/**
	 * 输入文字长度
	 */
	private int maxLength;

	/**
	 * 必填
	 */
	private boolean required;

	/**
	 * 顺序
	 */
	private transient int order;

	/**
	 * 校验用正则表达式
	 */
	private String regexp;

	/**
	 * 提示信息
	 */
	private String hint;

	/**
	 * 校验错误后的提示语
	 */
	private String errTip;

	/**
	 * 日期格式
	 */
	private String format;
	/**
	 * 各种属性
	 */
	private Map<String, String> attribute;
	/**
	 * 获取多选项的键值对
	 */
	private OptionResolver selectItmesMap;

	/**
	 * Style
	 */
	private Map<String, String> style;

	/**
	 * 自定义属性，不同的前端控件采用不同的解释。
	 */
	private Map<String, String> customAttr;

	/**
	 * 校验类型
	 */
	private String vtype;

	/**
	 * class属性
	 */
	private String cls = "auto-input";

	/**
	 * 标签的宽度（像素）。0表示采用框架默认值
	 */
	private int labelWidth = 0;

	/**
	 * 如果为true，说明输入框是Between输入。目前只支持在complextSearch中出现between输入
	 */
	private boolean betweenInput;

	/**
	 * 输入框之后
	 */
	private String afterInput;

	public FormFieldModel() {
	}

	public FormFieldModel(String name, String text) {
		this.code = name;
		this.name = text;

	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public InputType getType() {
		return type;
	}

	public void setType(InputType type) {
		this.type = type;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int compareTo(FormFieldModel o) {
		return Integer.compare(this.order, o.order);
	}

	public String getRegexp() {
		return regexp;
	}

	public void setRegexp(String regexp) {
		this.regexp = regexp;
	}

	public String getErrTip() {
		return errTip;
	}

	public void setErrTip(String errTip) {
		this.errTip = errTip;
	}

	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	public Map<String, String> getAttribute() {
		return attribute;
	}

	public void setAttribute(Map<String, String> attribute) {
		this.attribute = attribute;
	}

	/**
	 * 向HTML属性中添加一项数据
	 * 
	 * @param key
	 * @param value
	 */
	public void addAttribute(String key, String value) {
		if (attribute == null) {
			attribute = new HashMap<>();
		}
		attribute.put(key, value);
	}

	/**
	 * 向自定义属性中添加一项数据
	 * 
	 * @param key
	 * @param value
	 */
	public void addCustomAttr(String key, String value) {
		if (customAttr == null) {
			customAttr = new HashMap<>();
		}
		customAttr.put(key, value);
	}

	public Map<String, String> getStyle() {
		return style;
	}

	public void setStyle(Map<String, String> style) {
		this.style = style;
	}

	public void addStyle(String key, String value) {
		if (style == null) {
			style = new HashMap<>();
		}
		style.put(key, value);
	}

	public String getVtype() {
		return vtype;
	}

	public void setVtype(String vtype) {
		this.vtype = vtype;
	}

//	public Map<String, String> getSelectItmesMap() {
//		if (selectItmesMap == null) {
//			return SINGLE;
//		}else if(selectItmesMap.isConditional()) {
//			InterpretContext ctx = InterpretContext.current();
//			if(ctx!=null) {
//				ValueStack values = ctx.getValueStack();
//				return selectItmesMap.getValues(Collections.singletonMap("scenario",values.getValue("scenario")));	
//			}else {
//				//FIXME 什么情况下页面都没有也会调到这里。按理说应该不会啊!
//				System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//				return selectItmesMap.getValues(null);	
//			}
//		}else {
//			return selectItmesMap.getValues(null);	
//		}
//	}

	public void prepareSelectItmesMap(OptionResolver resolver) {
		this.selectItmesMap = resolver;
	}
}
