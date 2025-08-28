package io.github.xuse.framework.vaadin.support;

import java.lang.reflect.Field;

import com.github.xuse.querydsl.util.StringUtils;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.data.provider.CallbackDataProvider;

import io.github.xuse.jetui.annotation.ViewColumn;
import io.github.xuse.jetui.repository.ListDataProvider;

public class VaadinViews {
	@SuppressWarnings("unused")
	private static final <T> void addColumns(Grid<T> grid, Class<T> clz) {
		for (Field field : clz.getDeclaredFields()) {
			ViewColumn c = field.getAnnotation(ViewColumn.class);
			if (c == null) {
				continue;
			}
			Column<?> column = grid.addColumn(new FieldAccessor<>(field, c))
					.setHeader(nullIf(c.caption(), field.getName()));
		}
		grid.setSizeFull();
	}

	private static String nullIf(String label, String name) {
		return StringUtils.isEmpty(label) ? name : label;
	}

	public static <T, F> Grid<T> createGrid(Class<T> clz, ListDataProvider<T, F> repo) {
		Grid<T> grid = new Grid<>();
		grid.setDataProvider(new CallbackDataProvider<T, F>(
				(q) -> repo.list(q.getFilter(), q.getOffset(), q.getLimit()), (q) -> repo.count(q.getFilter())));
		addColumns(grid, clz);
		return grid;
	}
}
