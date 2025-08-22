package io.github.xuse.jetui.model;

import java.util.Iterator;
import java.util.List;

/**
 * 表单显示用模型
 * @author jiyi
 *
 */
public abstract class FormModel implements Iterable<FormFieldModel>{
	/**
	 * 表单中的各个字段
	 */
	private List<FormFieldModel> fields;
	/**
	 * 表单要显示的对象。可以为null
	 */
	protected Object obj;
	
	public FormModel(List<FormFieldModel> fields) {
		this.fields=fields;
	}

	/**
	 * 为表单中的所有输入控件统一设置Label width。
	 * @param value
	 */
	public void setLabelWidth(int value) {
		for (FormFieldModel field : fields) {
			field.setLabelWidth(value);
		}
	}

	public List<FormFieldModel> getFields() {
		return fields;
	}

	public void setFields(List<FormFieldModel> fields) {
		this.fields = fields;
	}

	public Object getObj() {
		return obj;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}
	
	
	@Override
	public Iterator<FormFieldModel> iterator() {
		return fields.iterator();
	}

	public abstract void loadData(String arg);
}
