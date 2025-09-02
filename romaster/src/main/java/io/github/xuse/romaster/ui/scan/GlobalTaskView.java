package io.github.xuse.romaster.ui.scan;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.framework.vaadin.support.AutoForm;
import io.github.xuse.framework.vaadin.support.VaadinForms;
import io.github.xuse.framework.vaadin.support.VaadinHelper;
import io.github.xuse.framework.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.obj.GlobalTask;
import io.github.xuse.romking.service.GlobalTaskService;
import jakarta.annotation.security.PermitAll;

@Route("task-list")
@PageTitle("Task List")
@Menu(order = 1, icon = "vaadin:clipboard-check", title = "Global Tasks")
@PermitAll
public class GlobalTaskView extends Main {

    final Grid<GlobalTask> taskGrid;
    final RomConsole console;
   

    public GlobalTaskView(RomConsole console) {
    	this.console=console;
    	
        add(VaadinHelper.viewToolbarBuilder(TaskForm.class)
        		.button("扫描", this::scanDialog)
        		.build());
        GlobalTaskService taskService = console.getBean(GlobalTaskService.class);
        add(taskGrid=VaadinViews.createGrid(GlobalTask.class, taskService));
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
        
    }
	
    private void scanDialog(ClickEvent<Button> event) {
    	AutoForm<TaskForm> form = VaadinForms.createAutoForm(new TaskForm(), TaskForm.class);
    	Dialog dialog = new Dialog();
    	dialog.setHeaderTitle("Scan");
    	Button closeButton = new Button(new Icon("lumo", "cross"),(e) -> dialog.close());
    	closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    	dialog.getHeader().add(closeButton);
    	
    	Button cancelButton = new Button("Cancel", (e) -> dialog.close());
    	cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    	dialog.getFooter().add(cancelButton);
    	
    	Button scanButton = new Button("Scan", this::doScan); //(e) -> dialog.close()
    	scanButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    //	scanButton.getStyle().set("margin-right", "auto");
    	dialog.getFooter().add(scanButton);
    }
    
    private void doScan(ClickEvent<Button> event) {
    	System.out.println("后台工作");
    }

}
