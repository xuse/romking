package io.github.xuse.jetui.vaadin.support;

import java.lang.reflect.Field;

import com.github.xuse.querydsl.util.Assert;
import com.github.xuse.querydsl.util.StringUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.dataview.GridDataView;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;

import io.github.xuse.jetui.annotation.ViewColumn;
import io.github.xuse.jetui.repository.ListDataProvider;
import lombok.Data;

public class VaadinViews {
	private VaadinViews() {
	}

	@SuppressWarnings("unused")
	private static final <T> void addColumns(Grid<T> grid, Class<T> clz) {
		for (Field field : clz.getDeclaredFields()) {
			ViewColumn c = field.getAnnotation(ViewColumn.class);
			if (c == null) {
				continue;
			}
			Column<?> column = grid.addColumn(new FieldAccessor<>(field, c))
					.setHeader(nullIf(c.caption(), field.getName()));
			if (c.sortable()) {
				column.setSortable(true);
			}

		}
		grid.setSizeFull();
	}

	public static <T, F> Grid<T> createGrid(Class<T> clz, ListDataProvider<T, F> repo) {
		Assert.notNull(clz);
		Assert.notNull(repo);
		Grid<T> grid = new Grid<>(clz, false);
		grid.setDataProvider(new CallbackDataProvider<T, F>(
				(q) -> repo.list(q.getFilter(), q.getOffset(), q.getLimit()), (q) -> repo.count(q.getFilter())));
		addColumns(grid, clz);
		return grid;
	}

	public static <T, F> Component createGridWithSimpleSearch(Class<T> clz, ListDataProvider<T, F> repo,F fileter) {
		SimpleFilter sf = new SimpleFilter();
		CallbackDataProvider<T, F> dataProvider = new CallbackDataProvider<T, F>(
				(q) -> repo.list(q.getFilter(), q.getOffset(), q.getLimit()), (q) -> repo.count(q.getFilter()));
		
		ConfigurableFilterDataProvider<T,Void,F> filterDataProvider = dataProvider.withConfigurableFilter();

		Grid<T> grid = new Grid<>(clz, false);
		addColumns(grid, clz);
		@SuppressWarnings("unused")
		GridDataView<T> dataView = grid.setItems(filterDataProvider);

		TextField searchField = new TextField();
		searchField.setWidth("50%");
		searchField.setPlaceholder("Search");
		searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
	    searchField.setValueChangeMode(ValueChangeMode.EAGER);
	    searchField.addValueChangeListener(e -> {
	    	sf.setText(e.getValue());
	        filterDataProvider.setFilter(fileter);
	    });
	    
	    VerticalLayout layout = new VerticalLayout(searchField, grid);
	    layout.setPadding(false);
		return layout;
	}

	public static <T, F> Component createGridWithSearchFilter(Class<T> clz, ListDataProvider<T, F> repo, F filter) {
		Grid<T> grid = new Grid<>(clz, false);
		grid.setDataProvider(new CallbackDataProvider<T, F>(
				(q) -> repo.list(q.getFilter(), q.getOffset(), q.getLimit()), (q) -> repo.count(q.getFilter())));
		addColumns(grid, clz);
		return grid;
	}

//	public static <T, F> Grid<T> createGridWithSearch(Class<T> clz, ListDataProvider<T, F> repo) {
//		Grid<T> grid = new Grid<>();
//		F currentFilter;
//		// 创建数据提供者（动态过滤）
//		DataProvider<T, F> dataProvider = new CallbackDataProvider<T, F>(
//				query -> repo.list(currentFilter, query.getOffset(), query.getLimit()),
//				query -> repo.count(currentFilter));
//		grid.setDataProvider(dataProvider);
//
//		// 创建查询表单
//		FormLayout form = new FormLayout();
//		List<Field> searchableFields = new ArrayList<>();
//		for (Field field : clz.getDeclaredFields()) {
//			ViewColumn c = field.getAnnotation(ViewColumn.class);
//			if (c != null && c.searchSimple()) {
//				searchableFields.add(field);
//				TextField textField = new TextField(c.caption());
//				form.add(textField);
//			}
//		}
//
//		// 提交按钮
//		Button searchButton = new Button("查询", event -> {
//			F newFilter = buildFilterFromForm(form, searchableFields);
//			currentFilter = newFilter;
//			dataProvider.refreshAll();
//		});
//		form.add(searchButton);
//
//		// 添加列
//		addColumns(grid, clz);
//
//		// 组合布局
//		return new VerticalLayout(form, grid);
//	}

//	private static <T, F> F buildFilterFromForm(FormLayout form, List<Field> fields) {
//		// 需要根据实际Filter类型实现，示例假设F是Map类型
//		Map<String, Object> filter = new HashMap<>();
//		for (int i = 0; i < fields.size(); i++) {
//			Field field = fields.get(i);
//			Component component = form.getComponentAt(i);
//			if (component instanceof TextField) {
//				filter.put(field.getName(), ((TextField) component).getValue());
//			}
//			// 其他控件类型可添加条件判断
//		}
//		return (F) filter; // 类型转换需根据实际类型调整
//	}

	private static String nullIf(String caption, String fieldName) {
		return StringUtils.isNotEmpty(caption) ? caption : fieldName;
	}

	@Data
	static class SimpleFilter {
		private String text;
	}
}
