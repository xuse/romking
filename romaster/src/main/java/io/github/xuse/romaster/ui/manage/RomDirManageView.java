package io.github.xuse.romaster.ui.manage;



import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.xuse.querydsl.util.DateFormats;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.framework.vaadin.support.VaadinHelper;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.obj.RomDirFilter;
import io.github.xuse.romking.service.RomImportService;
import jakarta.annotation.security.PermitAll;

@PageTitle("Roms Repositories")
@PermitAll 
public class RomDirManageView extends Main {

	@Autowired
    private RomImportService romImportService;
	
	@Autowired
	private RomDirRepository romRepos;
	
//    final TextField description;
//    final DatePicker dueDate;
//    final Button createBtn;
    final Grid<RomDir> taskGrid;

    public RomDirManageView() {
        taskGrid = new Grid<>();
    	add(taskGrid);

    	addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
        
        taskGrid.setDataProvider(this.createDataProvider());
    	VaadinHelper.addColumns(taskGrid,RomDir.class);
    	VaadinHelper.viewToolbarBuilder(RomDirFilter.class)
    	.build(taskGrid);


        
    }

    private DataProvider<RomDir, RomDirFilter> createDataProvider() {
    	return new CallbackDataProvider<>(
    			(q)->romRepos.list(q.getFilter(), q.getOffset(), q.getLimit()),
    			(q)-> romRepos.count(q.getFilter()));
	}

	private void createTask() {
//        taskService.createTask(description.getValue(), dueDate.getValue());
//        taskGrid.getDataProvider().refreshAll();
//        description.clear();
//        dueDate.clear();
//        Notification.show("Task added", 3000, Notification.Position.BOTTOM_END)
//                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

}
