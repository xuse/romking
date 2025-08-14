package io.github.xuse.romaster.ui.manage;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.xuse.querydsl.util.DateFormats;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.base.ui.component.ViewToolbar;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.service.RomRepoService;
import jakarta.annotation.security.PermitAll;

@Route("rom-mng")
@PageTitle("Roms Repositories")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Roms Repositories")
@PermitAll 
public class RomRepoManageView extends Main {

	@Autowired
    private final RomRepoService romRepos;

    final TextField description;
    final DatePicker dueDate;
    final Button createBtn;
    final Grid<RomDir> taskGrid;

    public RomRepoManageView() {
        description = new TextField();
        description.setPlaceholder("What do you want to do?");
        description.setAriaLabel("Task description");
        description.setMaxLength(255);
        description.setMinWidth("20em");

        dueDate = new DatePicker();
        dueDate.setPlaceholder("Due date");
        dueDate.setAriaLabel("Due date");

        createBtn = new Button("Create", event -> createTask());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        taskGrid = new Grid<>();
        taskGrid.setItems(query -> taskService.list(toSpringPageRequest(query)).stream());
        taskGrid.addColumn(RomDir::getDescription).setHeader("Description");
        taskGrid.addColumn(task -> Optional.ofNullable(task.getLastScan()).map(DateFormats.DATE_TIME_CS::format).orElse("Never"))
                .setHeader("Last Scan");
        taskGrid.addColumn(task -> DateFormats.DATE_TIME_CS.format(task.getCreateTime())).setHeader("Creation Date");
        taskGrid.setSizeFull();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Task List", ViewToolbar.group(description, dueDate, createBtn)));
        add(taskGrid);
    }

    private void createTask() {
        taskService.createTask(description.getValue(), dueDate.getValue());
        taskGrid.getDataProvider().refreshAll();
        description.clear();
        dueDate.clear();
        Notification.show("Task added", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

}
