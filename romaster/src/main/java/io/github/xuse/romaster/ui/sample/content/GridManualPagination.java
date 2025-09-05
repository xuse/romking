package io.github.xuse.romaster.ui.sample.content;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import io.github.xuse.jetui.vaadin.component.PaginationControls;
import io.github.xuse.romaster.ui.sample.obj.Person;

@Route("grid-manual-pagination")
public class GridManualPagination extends VerticalLayout {

	private final PaginationControls paginationControls = new PaginationControls();
	private final DataSource dataSource = new DataSource();

	private final DataProvider<Person, String> pagingDataProvider = DataProvider.fromFilteringCallbacks(query -> {
		query.getLimit();
		query.getOffset();
		var offset = paginationControls.calculateOffset();
		var limit = paginationControls.getPageSize();
		return dataSource.fetch(query.getFilter(), offset, limit);
	}, query -> {
		var itemCount = dataSource.count(query.getFilter());
		paginationControls.recalculatePageCount(itemCount);
		var offset = paginationControls.calculateOffset();
		var limit = paginationControls.getPageSize();
		var remainingItemsCount = itemCount - offset;
		return Math.min(remainingItemsCount, limit);
	});

	public GridManualPagination() {
		setPadding(false);
		Grid<Person> grid = new Grid<>(Person.class, false);
		grid.addColumn(createPersonRenderer()).setHeader("Name").setFlexGrow(0).setWidth("230px");
		grid.addColumn(Person::getEmail).setHeader("Email");
		grid.addColumn(Person::getProfession).setHeader("Profession");
		grid.setAllRowsVisible(true); // this will prevent scrolling in the grid
		var dataProvider = pagingDataProvider.withConfigurableFilter();
		grid.setDataProvider(dataProvider);
		paginationControls.onPageChanged(() -> grid.getDataProvider().refreshAll());
		final TextField searchField = createSearchField();
		searchField.addValueChangeListener(e -> {
			dataProvider.setFilter(e.getValue());
		});

		add(searchField, wrapWithVerticalLayout(grid, paginationControls));
	}

	@NotNull
	private VerticalLayout wrapWithVerticalLayout(Component component1, Component component2) {
		var gridWithPaginationLayout = new VerticalLayout(component1, component2);
		gridWithPaginationLayout.setPadding(false);
		gridWithPaginationLayout.setSpacing(false);
		gridWithPaginationLayout.getThemeList().add("spacing-xs");
		return gridWithPaginationLayout;
	}

	@NotNull
	private TextField createSearchField() {
		TextField searchField = new TextField();
		searchField.setWidth("50%");
		searchField.setPlaceholder("Search");
		searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		searchField.setValueChangeMode(ValueChangeMode.EAGER);
		return searchField;
	}

	public static class DataSource {
		private final List<Person> people = DataService.getPeople();

		public Stream<Person> fetch(Optional<String> searchTerm, int offset, int limit) {
			return people.stream().filter(person -> matchesSearchTerm(person, searchTerm.orElse(""))).skip(offset)
					.limit(limit);
		}

		public int count(Optional<String> searchTerm) {
			return (int) people.stream().filter(person -> matchesSearchTerm(person, searchTerm.orElse(""))).count();
		}

		public boolean matchesSearchTerm(Person person, String searchTerm) {
			return searchTerm == null || searchTerm.isEmpty()
					|| person.getFullName().toLowerCase().contains(searchTerm.toLowerCase())
					|| person.getEmail().toLowerCase().contains(searchTerm.toLowerCase())
					|| person.getProfession().toLowerCase().contains(searchTerm.toLowerCase());
		}
	}

	private static Renderer<Person> createPersonRenderer() {
		return LitRenderer
				.<Person>of("<vaadin-horizontal-layout style=\"align-items: center;\" theme=\"spacing\">"
						+ "  <vaadin-avatar img=\"${item.pictureUrl}\" name=\"${item.fullName}\"></vaadin-avatar>"
						+ "  <span> ${item.fullName} </span>" + "</vaadin-horizontal-layout>")
				.withProperty("pictureUrl", Person::getPictureUrl).withProperty("fullName", Person::getFullName);
	}

}