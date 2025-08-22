package io.github.xuse.framework.vaadin.support;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.github.xuse.querydsl.util.StringUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.annotation.ViewColumn;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.jetui.model.FormFieldModel;
import io.github.xuse.romking.repo.obj.RomDir;

public class VaadinHelper {
	
	private static final Map<InputType,Function<FormFieldModel, Component>> creaters=new HashMap<>();
	
	public static final <T> void addColumns(Grid<T> grid, Class<T> clz) {
		for(Field field:clz.getDeclaredFields()) {
			ViewColumn c=field.getAnnotation(ViewColumn.class);
			if(c==null) {
				continue;
			}
			grid.addColumn(new FieldAccessor<>(field,c)).setHeader(nullIf(c.caption(),field.getName()));
		}
		grid.setSizeFull();
	}

	private static String nullIf(String label, String name) {
		return StringUtils.isEmpty(label)?name:label;
	}

	public static ViewToolbarBuilder viewToolbarBuilder(Class<?> clz) {
	//	add(new ViewToolbar("Task List", ViewToolbar.group(description, dueDate, createBtn)));
		return new ViewToolbarBuilder(clz);
		
	}
	
//	{
//		description = new TextField();
//        description.setPlaceholder("What do you want to do?");
//        description.setAriaLabel("Task description");
//        description.setMaxLength(255);
//        description.setMinWidth("20em");
//
//        dueDate = new DatePicker();
//        dueDate.setPlaceholder("Due date");
//        dueDate.setAriaLabel("Due date");
//
//        createBtn = new Button("Create", event -> createTask());
//        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
//        new ViewToolbar("Task List", ViewToolbar.group(description, dueDate, createBtn))
//	}
//    
	
	//VaadinIcon;
	
	static {
		creaters.put(InputType.CHECKBOX, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.COMBO, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.DATE, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.FILE, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.HIDDEN, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.LABEL, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.NUMBER, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.PASSWORD, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.RADIO, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.READONLY, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.TEXT, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.TEXTAREA, (e)->{
			return new Checkbox();
		});
		creaters.put(InputType.TREE_COMBO, (e)->{
			return new Checkbox();
		});
	}
	
	
	public class ViewToolbarBuilder {
		private final Class<?> clz;
		public ViewToolbarBuilder(Class<?> clz) {
			this.clz=clz;
			init();
		}
		
		public void init() {
			List<Component> components=new ArrayList<>();
			for(Field field:clz.getDeclaredFields()) {
				FormField ff=field.getAnnotation(FormField.class);
				components.add(createField(ff,field));
			}
		}

		private Component createField(FormFieldModel model) {
			return creaters.get(model.getType()).create(model);
		}

		public Component build(Grid<RomDir> taskGrid) {
			
			return null;
		}
	}
	
}
