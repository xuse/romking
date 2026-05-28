package io.github.xuse.romaster.ui.manage;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.vo.RomRepo;
import io.github.xuse.romking.service.RomMngService;
import jakarta.annotation.security.PermitAll;

/**
 * 我的ROM - 首页视图。
 * 显示所有仓库列表，点击仓库名进入该仓库的ROM文件列表。
 */
@Route("rom-mng")
@PageTitle("我的ROM")
@Menu(order = 0, icon = "vaadin:home", title = "我的ROM")
@PermitAll
public class RomRepoManageView extends Main {

	public RomRepoManageView(RomConsole console) {
		RomMngService romService = console.getBean(RomMngService.class);

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

		Grid<RomRepo> grid = VaadinViews.createGrid(RomRepo.class, romService);
		grid.setSizeFull();

		// 将仓库名称列替换为可点击链接
		var labelCol = grid.getColumnByKey("label");
		if (labelCol != null) {
			labelCol.setRenderer(new ComponentRenderer<>(repo -> {
				RouterLink link = new RouterLink(repo.getLabel(), RepoRomListView.class, repo.getLabel());
				link.getStyle().set("color", "var(--lumo-primary-text-color)");
				link.getStyle().set("font-weight", "500");
				return link;
			}));
		}

		add(grid);
	}
}
