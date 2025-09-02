package io.github.xuse.romaster.ui.sample;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import lombok.Data;

@Route("grid-column-filtering")
public class GridColumnFiltering extends Div {

	public GridColumnFiltering() {
		Grid<Person> grid = new Grid<>(Person.class, false);
		Grid.Column<Person> nameColumn = grid.addColumn(createPersonRenderer()).setWidth("230px").setFlexGrow(0);
		Grid.Column<Person> emailColumn = grid.addColumn(Person::getEmail);
		Grid.Column<Person> professionColumn = grid.addColumn(Person::getProfession);

		List<Person> people = getPeople();
		GridListDataView<Person> dataView = grid.setItems(people);
		PersonFilter personFilter = new PersonFilter(dataView);

		grid.getHeaderRows().clear();
		HeaderRow headerRow = grid.appendHeaderRow();

		headerRow.getCell(nameColumn).setComponent(createFilterHeader("Name", personFilter::setFullName));
		headerRow.getCell(emailColumn).setComponent(createFilterHeader("Email", personFilter::setEmail));
		headerRow.getCell(professionColumn).setComponent(createFilterHeader("Profession", personFilter::setProfession));

		add(grid);
	}

	private List<Person> getPeople() {
		Person p1 = new Person();
		p1.setEmail("test@sssd.ccom");
		p1.setFullName("zhangsan");
		p1.setPictureUrl(VaadinIcon.ABSOLUTE_POSITION.toString());
		p1.setProfession("Dr.");

		Person p2 = new Person();
		p2.setEmail("test@sssd.ccom");
		p2.setFullName("Zhangfei");
		p2.setPictureUrl(VaadinIcon.AIRPLANE.toString());
		p2.setProfession("Dr.");

		Person p3 = new Person();
		p3.setEmail("test2@sssd.ccom");
		p3.setFullName("Wangwu");
		p3.setPictureUrl(VaadinIcon.ACADEMY_CAP.toString());
		p3.setProfession("Dr.");
		return Arrays.asList(p1, p2, p3);
	}

	private static Component createFilterHeader(String labelText, Consumer<String> filterChangeConsumer) {
		NativeLabel label = new NativeLabel(labelText);
		label.getStyle().set("padding-top", "var(--lumo-space-m)").set("font-size", "var(--lumo-font-size-xs)");
		TextField textField = new TextField();
		textField.setValueChangeMode(ValueChangeMode.EAGER);
		textField.setClearButtonVisible(true);
		textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
		textField.setWidthFull();
		textField.getStyle().set("max-width", "100%");
		textField.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
		VerticalLayout layout = new VerticalLayout(label, textField);
		layout.getThemeList().clear();
		layout.getThemeList().add("spacing-xs");

		return layout;
	}

	private static class PersonFilter {
		private final GridListDataView<Person> dataView;

		private String fullName;
		private String email;
		private String profession;

		public PersonFilter(GridListDataView<Person> dataView) {
			this.dataView = dataView;
			this.dataView.addFilter(this::test);
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
			this.dataView.refreshAll();
		}

		public void setEmail(String email) {
			this.email = email;
			this.dataView.refreshAll();
		}

		public void setProfession(String profession) {
			this.profession = profession;
			this.dataView.refreshAll();
		}

		public boolean test(Person person) {
			boolean matchesFullName = matches(person.getFullName(), fullName);
			boolean matchesEmail = matches(person.getEmail(), email);
			boolean matchesProfession = matches(person.getProfession(), profession);

			return matchesFullName && matchesEmail && matchesProfession;
		}

		private boolean matches(String value, String searchTerm) {
			return searchTerm == null || searchTerm.isEmpty() || value.toLowerCase().contains(searchTerm.toLowerCase());
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