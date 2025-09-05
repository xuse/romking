package io.github.xuse.romaster.ui.sample.content;
import java.util.List;
import java.util.stream.Stream;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;

import io.github.xuse.romaster.ui.sample.obj.Person;

@Route("grid-item-details-toggle")
public class GridItemDetailsToggle extends Div {

    public GridItemDetailsToggle() {
        Grid<Person> grid = new Grid<>(Person.class, false);
        grid.addColumn(createToggleDetailsRenderer(grid)).setWidth("80px")
                .setFlexGrow(0).setFrozen(true);
        grid.addColumn(Person::getFullName).setHeader("Name");
        grid.addColumn(Person::getProfession).setHeader("Profession");

        grid.setDetailsVisibleOnClick(false);
        grid.setItemDetailsRenderer(createPersonDetailsRenderer());

        List<Person> people = DataService.getPeople();
        grid.setItems(people);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        add(grid);
    }

    //创建折叠小图标，并绑定折叠/展开事件。
    private static Renderer<Person> createToggleDetailsRenderer(
            Grid<Person> grid) {
    	//这个示例展示了在原生react中调用Java代码中的事件 
        return LitRenderer.<Person> of("""
                    <vaadin-button
                        theme="tertiary icon"
                        aria-label="Toggle details"
                        aria-expanded="${model.detailsOpened ? 'true' : 'false'}"
                        @click="${handleClick}"
                    >
                        <vaadin-icon .icon="${model.detailsOpened ? 'lumo:angle-down' : 'lumo:angle-right'}"></vaadin-icon>
                    </vaadin-button>
                """)
                .withFunction("handleClick",person -> grid.setDetailsVisible(person,!grid.isDetailsVisible(person)));
    }

    //将详情表单成为一个表格列的渲染器
    private static ComponentRenderer<PersonDetailsFormLayout, Person> createPersonDetailsRenderer() {
        return new ComponentRenderer<>(PersonDetailsFormLayout::new,PersonDetailsFormLayout::setPerson);
    }

    //创建详情表单
    private static class PersonDetailsFormLayout extends FormLayout {
        private final TextField emailField = new TextField("Email address");
        private final TextField phoneField = new TextField("Phone number");
        private final TextField streetField = new TextField("Street address");
        private final TextField zipField = new TextField("ZIP code");
        private final TextField cityField = new TextField("City");
        private final TextField stateField = new TextField("State");

        public PersonDetailsFormLayout() {
            Stream.of(emailField, phoneField, streetField, zipField, cityField,
                    stateField).forEach(field -> {
                        field.setReadOnly(true);
                        add(field);
                    });

            setResponsiveSteps(new ResponsiveStep("0", 3));
            setColspan(emailField, 3);
            setColspan(phoneField, 3);
            setColspan(streetField, 3);
        }

        public void setPerson(Person person) {
            emailField.setValue(person.getEmail());
            phoneField.setValue(person.getAddress().getPhone());
            streetField.setValue(person.getAddress().getStreet());
            zipField.setValue(person.getAddress().getZip());
            cityField.setValue(person.getAddress().getCity());
            stateField.setValue(person.getAddress().getState());
        }
    }

}