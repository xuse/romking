package io.github.xuse.framework.vaadin.support;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.github.xuse.querydsl.util.TypeUtils;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;

import io.github.xuse.jetui.common.InputType;
import io.github.xuse.jetui.model.FormFieldModel;

public class VaadinForms {
	private static final Map<InputType,Function<FormFieldModel, AbstractField<?,?>>> creaters=new HashMap<>();
	
	
	
	public static <T> GenericForm<T> createBinderForom(Class<T> clz) {
		return createBinderForom(TypeUtils.newInstance(clz),clz);
	}
	
	public static <T> GenericForm<T> createBinderForom(T object, Class<T> clz) {
		GenericForm<T> form=new GenericForm<>(object,clz);
		form.scanFields();
		return form;
	}	
	
	public static AbstractField<?,?> createField(FormFieldModel model) {
		return creaters.get(model.getType()).apply(model);
	}
	
	private static TextField text(FormFieldModel model){
		TextField field = new TextField();
        field.setPlaceholder(model.getPlaceHolder());
        field.setAriaLabel(model.getLabel());
        if(model.getLabelWidth()>0)
        	field.setMaxLength(model.getLabelWidth());
        field.setMinWidth("20em");
		return field;
	}
	private static DatePicker date(FormFieldModel model){
		DatePicker field = new DatePicker();
		field.setPlaceholder(model.getPlaceHolder());
	    field.setAriaLabel(model.getPlaceHolder());
		return field;
	}
	private static Select<String> combo(FormFieldModel model){
		Select<String> combo=new Select<>();
		return combo;
	}
	private static TextArea textarea(FormFieldModel model){
		TextArea ta=new TextArea();
		return ta;
	}
	private static TextField file(FormFieldModel model){
		return null;
	}
	private static PasswordField password(FormFieldModel model){
		PasswordField pf=new PasswordField();
		return pf;
	}
	private static NumberField number(FormFieldModel model){
		NumberField field=new NumberField();
		return field;
	}
	private static TextField hidden(FormFieldModel model){
		TextField field = new TextField();
		
		return field;
	}
	private static RadioButtonGroup<String> radio(FormFieldModel model){
		RadioButtonGroup<String> radioGroup = new RadioButtonGroup<>();
		//radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		radioGroup.setLabel(model.getLabel());
		//radioGroup.setItems("Economy", "Business", "First Class");
		if(model.isRequired()) {
			radioGroup.setRequired(true);
		}
		
//		if(mode.)
		return radioGroup;
	}
	private static MenuBar label(FormFieldModel model){
		MenuBar menuBar = new MenuBar();
		MenuItem item = menuBar.addItem("Value");
		//SubMenu subMenu = item.getSubMenu();
		return menuBar;
	}
	private static TimePicker time(FormFieldModel model){
		TimePicker field=new TimePicker();
		return field;
	}
	private static EmailField email(FormFieldModel model){
		EmailField field=new EmailField();
		return field;
	}
	private static ComboBox<String> comboTree(FormFieldModel model){
		ComboBox<String> combo=new ComboBox<>();
		return combo;
	}
	private static Checkbox checkbox(FormFieldModel model){
		Checkbox c=new Checkbox();
		return c;
	}
	
	static {
		creaters.put(InputType.CHECKBOX, VaadinForms::checkbox);
		creaters.put(InputType.COMBO, VaadinForms::combo);
		creaters.put(InputType.DATE, VaadinForms::date);
		creaters.put(InputType.FILE, VaadinForms::file);
		creaters.put(InputType.HIDDEN, VaadinForms::hidden);
//		creaters.put(InputType.LABEL, VaadinForms::label);
		creaters.put(InputType.NUMBER,VaadinForms::number);
		creaters.put(InputType.PASSWORD, VaadinForms::password);
		creaters.put(InputType.RADIO, VaadinForms::radio);
		creaters.put(InputType.TIME,VaadinForms::email);
		creaters.put(InputType.EMAIL,VaadinForms::time);
		creaters.put(InputType.TEXT, VaadinForms::text);
		creaters.put(InputType.TEXTAREA, VaadinForms::textarea);
		//creaters.put(InputType.TREE_COMBO, VaadinForms::comboTree);
	}
	
}
