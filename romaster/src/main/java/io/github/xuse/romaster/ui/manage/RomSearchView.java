package io.github.xuse.romaster.ui.manage;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.support.VaadinHelper;
import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.repo.obj.RomFileFilter;
import jakarta.annotation.security.PermitAll;

/**
 * ROM全局搜索视图。
 * 支持按游戏名、平台、区域、类型组合筛选，跨所有仓库搜索。
 */
@Route("rom-search")
@PageTitle("ROM Search")
@Menu(order = 3, icon = "vaadin:search", title = "ROM Search")
@PermitAll
public class RomSearchView extends Main {

	private final RomFileRepository romFileRepo;
	private Grid<RomFile> grid;

	public RomSearchView(RomConsole console) {
		this.romFileRepo = console.getBean(RomFileRepository.class);

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

		// 搜索工具栏（使用RomFileFilter的@FormField注解生成输入框）
		add(VaadinHelper.viewToolbarBuilder(RomFileFilter.class)
				.name("ROM搜索")
				.button("查询", this::doSearch)
				.build());

		// 数据Grid（不带dirId过滤，展示所有ROM）
		grid = VaadinViews.createGrid(RomFile.class, romFileRepo);
		grid.setSizeFull();
		add(grid);
	}

	private void doSearch(ClickEvent<Button> event) {
		// TODO: 从工具栏获取筛选条件后刷新Grid
		grid.getDataProvider().refreshAll();
	}
}
