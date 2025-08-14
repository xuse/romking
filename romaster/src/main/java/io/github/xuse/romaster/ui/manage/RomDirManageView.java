package io.github.xuse.romaster.ui.manage;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;
import static org.mockito.Mockito.description;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.xuse.querydsl.util.DateFormats;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.base.ui.component.ViewToolbar;
import io.github.xuse.romking.repo.dal.RomRepoRepository;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.service.RomRepoService;
import jakarta.annotation.security.PermitAll;

@Route("rom-mng")
@PageTitle("Roms Repositories")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Roms Repositories")
@PermitAll 
public class RomDirManageView extends Main {

	@Autowired
    private final RomRepoService romRepoService;
	
	@Autowired
	private final RomRepoRepository romRepos;

//    final TextField description;
//    final DatePicker dueDate;
//    final Button createBtn;
    final Grid<RomDir> taskGrid;

    public RomDirManageView() {
        taskGrid = new Grid<>();
    	add(taskGrid);
    	
    	taskGrid.setPagination(true);
    	
    	
    	QueryDataProvider<RomDir, Void> dataProvider = QueryDataProvider.ofQuery(query -> {
    		return yourDatabaseQuery(query.getOffset(), query.getLimit(), query.getSortOrders());
    	});
    	taskGrid.setDataProvider(dataProvider);

    	

Pagination pagination = new Pagination(0, 10); // 假设每页显示10条数据
pagination.setHasArrows(true);
pagination.addPageChangeListener(event -> {
    dataProvider.getFetchConfiguration().setOffset(pagination.getCurrentPage() * pagination.getPageSize());
    dataProvider.refreshAll(); // 刷新数据提供者以显示新页面数据
});
add(pagination);


grid.addColumn(YourDataType::getName).setSortable(true); // 使某列可排序
grid.setFilterable(true); // 使Grid整体可过滤（需要支持过滤的数据提供者）

grid.setWidthFull(); // 使Grid宽度充满父容器
add(grid, pagination); // 先添加Grid，后添加Pagination，确保布局正确显示在下面。

    	
        taskGrid.setItems(query -> romRepos.list(toSpringPageRequest(query)).stream());
        taskGrid.addColumn(RomDir::getDescription).setHeader("Description");
        taskGrid.addColumn(task -> Optional.ofNullable(task.getLastScan()).map(DateFormats.DATE_TIME_CS::format).orElse("Never"))
                .setHeader("Last Scan");
        taskGrid.addColumn(task -> DateFormats.DATE_TIME_CS.format(task.getCreateTime())).setHeader("Creation Date");
        taskGrid.setSizeFull();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Task List", ViewToolbar.group(description, dueDate, createBtn)));
        
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
