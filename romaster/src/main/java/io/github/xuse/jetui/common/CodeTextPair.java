package io.github.xuse.jetui.common;

import java.io.Serializable;


/**
 * 键值对
 * @author jiyi
 *
 */
public class CodeTextPair implements Comparable<CodeTextPair>, Serializable {
	private static final long serialVersionUID = -2213222495346597158L;

	private String code;
	
	private String text;

	public CodeTextPair() {
	}

	public CodeTextPair(String code, String text) {
		this.text = text;
		this.code = code;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		if (text == null)
			return code;
		if (code == null)
			return text;
		return new StringBuilder(text.length() + code.length() + 2).append('[').append(code).append(']').append(text).toString();
	}

	@Override
	public int hashCode() {
		if (text == null && code == null) {
			return 0;
		} else if (text == null) {
			return code.hashCode();
		} else if (code == null) {
			return text.hashCode();
		} else {
			return text.hashCode() + code.hashCode();
		}
	}

	public int compareTo(CodeTextPair o) {
		if (o == null)
			return 1;
		if (o.code == null)
			return 1;
		return this.code.compareTo(o.code);
	}

}
