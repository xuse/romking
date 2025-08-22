package io.github.xuse.jetui.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于生成HTML中的&lt;a&gt;元素
 * @author jiyi
 *
 */
public class A {
	private String href = "javascript:void(0);";
	private String text;
	private String onclick;
	private Map<String, Object> attribute;

	public A(String text) {
		this.text = text;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getOnclick() {
		return onclick;
	}

	public void setOnclick(String onclick) {
		this.onclick = onclick;
	}

	public void setAttribute(String key, Object value) {
		if (attribute == null) {
			attribute = new HashMap<String, Object>();
		}
		attribute.put(key, value);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<a href=\"").append(href).append("\" ");
		if (onclick != null) {
			sb.append("onclick=\"").append(escapeQuot2(onclick)).append("\" ");
		}
		if (attribute != null) {
			for (Map.Entry<String, Object> entry : attribute.entrySet()) {
				sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\" ");
			}
		}
		sb.append('>').append(text).append("</a>");
		return sb.toString();
	}
	
	/**
	 * 将HTML中的双引号替换为转义后的双引号
	 * @param s
	 * @return
	 */
	private static String escapeQuot2(String s) {
		return s.replace("\"", "\\\"");
	}
}
