package io.github.xuse.framework.vaadin.support;

import java.lang.reflect.Field;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.model.FormFieldModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutoForm<T> extends Composite<FormLayout>{
	private T bean;
	
	private Class<T> clz;
	
	private final Binder<T> binder;
	
	public AutoForm(T bean,Class<T> clz) {
		this.bean=bean;
		this.clz=clz;
		this.binder = new Binder<T>();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void scanFields() {
		for(Field field:clz.getDeclaredFields()) {
			FormField ff=field.getAnnotation(FormField.class);
			AbstractField component=VaadinForms.createField(FormFieldModel.of(ff, field.getName(), field));
			FieldAccessor accessor=new FieldAccessor<>(field, null);
			binder.bind(component, accessor, accessor);
			getContent().add(component);
		}
		refreshUI();
	}
	
	public void refreshUI() {
		 binder.readBean(bean);
	}
	
	public void refreshEmpty() {
		 binder.refreshFields();
	}
	
	public void attach(T bean) {
		this.bean=bean;
		binder.readBean(bean);
	}
	
	public T getBeanIfValid() {
		if(binder.writeBeanIfValid(bean)) {
			return bean;
		}else {
			return null;
		}
	}
}
