package io.github.xuse.framework.vaadin.support;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.github.xuse.querydsl.util.StringUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import io.github.xuse.base.ui.component.ViewToolbar;
import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.annotation.ViewColumn;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.jetui.model.FormFieldModel;

public class VaadinHelper {
	
	private static final Map<InputType,Function<FormFieldModel, Component>> creaters=new HashMap<>();
	
	@SuppressWarnings("unused")
	public static final <T> void addColumns(Grid<T> grid, Class<T> clz) {
		for(Field field:clz.getDeclaredFields()) {
			ViewColumn c=field.getAnnotation(ViewColumn.class);
			if(c==null) {
				continue;
			}
			Column<?> column = grid.addColumn(new FieldAccessor<>(field,c)).setHeader(nullIf(c.caption(),field.getName()));
		}
		grid.setSizeFull();
	}

	private static String nullIf(String label, String name) {
		return StringUtils.isEmpty(label)?name:label;
	}

	public static ViewToolbarBuilder viewToolbarBuilder(Class<?> clz) {
		return new ViewToolbarBuilder(clz);
	}
	
	private static TextField text(FormFieldModel model){
		TextField description = new TextField();
        description.setPlaceholder(model.getPlaceHolder());
        description.setAriaLabel(model.getLabel());
        description.setMaxLength(model.getLabelWidth());
        description.setMinWidth("20em");
		return description;
	}
	private static DatePicker date(FormFieldModel model){
		DatePicker dueDate = new DatePicker();
	    dueDate.setPlaceholder("Due date");
	    dueDate.setAriaLabel("Due date");
		return dueDate;
	}
	private static ComboBox<String> combo(FormFieldModel model){
		ComboBox<String> combo=new ComboBox<>();
		
		return combo;
	}
	private static TextArea textarea(FormFieldModel model){
		TextArea ta=new TextArea();
		return ta;
	}
	private static Component file(FormFieldModel model){
		return null;
	}
	private static Component readonly(FormFieldModel model){
		return null;
	}
	private static Component password(FormFieldModel model){
		return null;
	}
	private static Component number(FormFieldModel model){
		return null;
	}
	private static Component hidden(FormFieldModel model){
		return null;
	}
	private static Component radio(FormFieldModel model){
		return null;
	}
	private static Component label(FormFieldModel model){
		return null;
	}
	private static Component comboTree(FormFieldModel model){
		return null;
	}
	private static Checkbox checkbox(FormFieldModel model){
		Checkbox c=new Checkbox();
		
		
		
		
		return c;
	}
	
	//VaadinIcon;
	
	static {
		creaters.put(InputType.CHECKBOX, VaadinHelper::checkbox);
		creaters.put(InputType.COMBO, VaadinHelper::combo);
		creaters.put(InputType.DATE, VaadinHelper::date);
		creaters.put(InputType.FILE, VaadinHelper::file);
		creaters.put(InputType.HIDDEN, VaadinHelper::hidden);
		creaters.put(InputType.LABEL, VaadinHelper::label);
		creaters.put(InputType.NUMBER,VaadinHelper::number);
		creaters.put(InputType.PASSWORD, VaadinHelper::password);
		creaters.put(InputType.RADIO, VaadinHelper::radio);
		creaters.put(InputType.READONLY,VaadinHelper::readonly);
		creaters.put(InputType.TEXT, VaadinHelper::text);
		creaters.put(InputType.TEXTAREA, VaadinHelper::textarea);
		//creaters.put(InputType.TREE_COMBO, VaadinHelper::comboTree);
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
				components.add(createField(FormFieldModel.of(ff, field.getName(), field)));
			}
			this.components=components;
		}
		
		public ViewToolbarBuilder name(String name) {
			this.name=name;
			return this;
		}

		private Component createField(FormFieldModel model) {
			return creaters.get(model.getType()).apply(model);
		}

		public Component build() {
			return new ViewToolbar(name, ViewToolbar.group(components.toArray(new Component[0])));
		}
	}
	
}
