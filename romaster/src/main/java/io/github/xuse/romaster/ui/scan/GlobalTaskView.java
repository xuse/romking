package io.github.xuse.romaster.ui.scan;

import javax.annotation.Resource;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.framework.vaadin.support.VaadinHelper;
import io.github.xuse.framework.vaadin.support.VaadinViews;
import io.github.xuse.romking.repo.obj.GlobalTask;
import io.github.xuse.romking.service.GlobalTaskService;
import jakarta.annotation.security.PermitAll;

@Route("task-list")
@PageTitle("Task List")
@Menu(order = 1, icon = "vaadin:clipboard-check", title = "Global Tasks")
@PermitAll
public class GlobalTaskView extends Main {

	@Resource
    private GlobalTaskService taskService;
    final Grid<GlobalTask> taskGrid;

    public GlobalTaskView() {
        add(VaadinHelper.viewToolbarBuilder(TaskForm.class)
        		.button("扫描", this::scan)
        		.build());
        
        add(taskGrid=VaadinViews.createGrid(GlobalTask.class, taskService));
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
        
    }
	
    private DataProvider<GlobalTask, Void> createDataProvider() {
    	return new CallbackDataProvider<>(
    			(q)->taskService.list(q.getFilter(), q.getOffset(), q.getLimit()),
    			(q)-> taskService.count(q.getFilter()));
	}
    
    
    
    private void scan(ClickEvent<Button> e) {
//        taskService.createTask(description.getValue(), dueDate.getValue());
//        taskGrid.getDataProvider().refreshAll();
//        description.clear();
//        dueDate.clear();
//        Notification.show("Task added", 3000, Notification.Position.BOTTOM_END)
//                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

}
