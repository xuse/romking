package io.github.xuse.romaster.ui.manage;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.framework.vaadin.support.VaadinHelper;
import io.github.xuse.framework.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.obj.RomDirFilter;
import io.github.xuse.romking.service.RomImportService;
import jakarta.annotation.security.PermitAll;

@PageTitle("Roms Repositories")
@PermitAll
public class RomDirManageView extends Main {
	final Grid<RomDir> taskGrid;
	final RomConsole romconsole;

	private void searchOnClick(ClickEvent<Button> event) {

	}

	public RomDirManageView(RomConsole console) {
		this.romconsole = console;

		RomImportService romImportService = console.getBean(RomImportService.class);
		RomDirRepository romRepos = console.getBean(RomDirRepository.class);

		add(VaadinHelper.viewToolbarBuilder(RomDirFilter.class).button("查询", this::searchOnClick).build());

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

		taskGrid = VaadinViews.createGrid(RomDir.class, romRepos);
		add(taskGrid);
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
