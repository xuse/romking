package io.github.xuse.romaster.ui.manage;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.vo.RomRepo;
import io.github.xuse.romking.service.RomMngService;
import jakarta.annotation.security.PermitAll;

/**
 * 仓库管理主视图。
 * 展示所有仓库（按label聚合），点击进入该仓库的目录列表。
 */
@Route("rom-mng")
@PageTitle("Roms Repositories")
@Menu(order = 0, icon = "vaadin:mailbox", title = "Roms Repositories")
@PermitAll
public class RomRepoManageView extends Main {

	final Grid<RomRepo> grid;

	public RomRepoManageView(RomConsole console) {
		RomMngService romService = console.getBean(RomMngService.class);

		grid = VaadinViews.createGrid(RomRepo.class, romService);
		grid.setSizeFull();

		// 点击仓库行进入目录列表
		grid.addItemClickListener(event -> {
			RomRepo repo = event.getItem();
			UI.getCurrent().navigate("rom-dirs/" + repo.getLabel());
		});

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
		add(grid);
	}
}
