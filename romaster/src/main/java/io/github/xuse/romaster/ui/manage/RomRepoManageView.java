package io.github.xuse.romaster.ui.manage;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.vo.RomRepo;
import io.github.xuse.romking.service.RomMngService;
import jakarta.annotation.security.PermitAll;

@Route("rom-mng")
@PageTitle("Roms Repositories")
@Menu(order = 0, icon = "vaadin:mailbox", title = "Roms Repositories")
@PermitAll 
public class RomRepoManageView extends Main {

	@Autowired
    private RomMngService romServicer;
	
	@Autowired
	private RomDirRepository romDirRepo;

    final Grid<RomRepo> taskGrid;
    
//    final TextField description;
//    final DatePicker dueDate;
//    final Button createBtn;

    public RomRepoManageView() {
//        description = new TextField();
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

        taskGrid = new Grid<>();
        taskGrid.setItems(query -> romServicer.listRepos(query.getLimit(),query.getOffset(),query.getFilter()).stream());
        taskGrid.addColumn(RomRepo::getLabel).setHeader("仓库名称");
        
        taskGrid.addColumn(RomRepo::getDirs).setHeader("目录数");
        taskGrid.addColumn(RomRepo::getRoms).setHeader("ROM数");
        taskGrid.addColumn(RomRepo::getMedias).setHeader("文件数");
        //添加操作列
        //taskGrid.addColumn(//null)
        taskGrid.setSizeFull();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        //创建视图工具条
//        add(new ViewToolbar("Task List", ViewToolbar.group(description, dueDate, createBtn)));
        add(taskGrid);
    }

//    private void createTask() {
//        taskService.createTask(description.getValue(), dueDate.getValue());
//        taskGrid.getDataProvider().refreshAll();
//        description.clear();
//        dueDate.clear();
//        Notification.show("Task added", 3000, Notification.Position.BOTTOM_END)
//                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
//    }

}
