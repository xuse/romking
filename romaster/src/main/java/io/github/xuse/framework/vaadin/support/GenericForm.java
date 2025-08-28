package io.github.xuse.framework.vaadin.support;

import java.lang.reflect.Field;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.model.FormFieldModel;
import lombok.Data;

@Data
public class GenericForm<T> extends Composite<FormLayout>{
	private T bean;
	
	private Class<T> clz;
	
	private final Binder<T> binder;
	
	//private Map<String,AbstractField<?,?>> fields=new HashMap<>();
	
	public GenericForm(T bean,Class<T> clz) {
		this.bean=bean;
		this.clz=clz;
		this.binder = new Binder<T>();
	}

	void scanFields() {
		for(Field field:clz.getDeclaredFields()) {
			FormField ff=field.getAnnotation(FormField.class);
			AbstractField component=VaadinForms.createField(FormFieldModel.of(ff, field.getName(), field));
			FieldAccessor accessor=new FieldAccessor<>(field, null);
			//add(component);
			binder.bind(component, accessor, accessor);
		}
	}
	
	

}
