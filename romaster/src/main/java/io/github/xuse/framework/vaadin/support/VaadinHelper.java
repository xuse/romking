package io.github.xuse.framework.vaadin.support;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import io.github.xuse.base.ui.component.ViewToolbar;
import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.model.FormFieldModel;

public class VaadinHelper {

	public static ViewToolbarBuilder viewToolbarBuilder(Class<?> clz) {
		return new ViewToolbarBuilder(clz);
	}
	
	public static class ViewToolbarBuilder {
		private final Class<?> clz;
		private String name;
		private List<Component> components;
		
		public ViewToolbarBuilder(Class<?> clz) {
			this.clz=clz;
			init();
		}
		
		public ViewToolbarBuilder button(String name,ComponentEventListener<ClickEvent<Button>> clickListener) {
			Button createBtn = new Button(name, clickListener);
	        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
	        components.add(createBtn);
			return this;
		}
	
		
		public void init() {
			List<Component> components=new ArrayList<>();
			for(Field field:clz.getDeclaredFields()) {
				FormField ff=field.getAnnotation(FormField.class);
				components.add(VaadinForms.createField(FormFieldModel.of(ff, field.getName(), field)));
			}
			this.components=components;
		}
		
		public ViewToolbarBuilder name(String name) {
			this.name=name;
			return this;
		}

		public Component build() {
			return new ViewToolbar(name, ViewToolbar.group(components.toArray(new Component[0])));
		}
	}
	
}
