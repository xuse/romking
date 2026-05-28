package io.github.xuse.romaster.ui.manage;

import java.util.List;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.component.PaginationControls;
import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.obj.RomFile;
import jakarta.annotation.security.PermitAll;

/**
 * 仓库ROM文件列表视图。
 * 显示指定仓库下所有ROM文件，支持按平台过滤和分页。
 */
@Route("repo-roms")
@PageTitle("仓库ROM列表")
@PermitAll
public class RepoRomListView extends Main implements HasUrlParameter<String> {

	private final RomConsole console;
	private String repoLabel;

	public RepoRomListView(RomConsole console) {
		this.console = console;
		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
	}

	@Override
	public void setParameter(BeforeEvent event, String parameter) {
		this.repoLabel = parameter;
		buildUI();
	}

	private void buildUI() {
		removeAll();
		RomFileRepository romFileRepo = console.getBean(RomFileRepository.class);

		// 标题 + 返回
		HorizontalLayout header = new HorizontalLayout();
		header.setAlignItems(HorizontalLayout.Alignment.CENTER);
		header.setSpacing(true);
		RouterLink backLink = new RouterLink("\u2190 返回仓库列表", RomRepoManageView.class);
		Span title = new Span("仓库: " + repoLabel);
		title.getStyle().set("font-size", "var(--lumo-font-size-l)");
		title.getStyle().set("font-weight", "bold");
		header.add(backLink, title);

		// 平台过滤
		Select<Platform> platformSelect = new Select<>();
		platformSelect.setPlaceholder("全部平台");
		platformSelect.setAriaLabel("平台过滤");
		platformSelect.setEmptySelectionAllowed(true);
		List<Platform> platforms = romFileRepo.findPlatformsByRepo(repoLabel);
		platformSelect.setItems(platforms);

		// 游戏名搜索
		TextField searchField = new TextField();
		searchField.setPlaceholder("搜索游戏名");
		searchField.setAriaLabel("游戏名搜索");
		searchField.setClearButtonVisible(true);
		searchField.setMinWidth("15em");

		HorizontalLayout toolbar = new HorizontalLayout(platformSelect, searchField);
		toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);

		// 分页
		PaginationControls paginationControls = new PaginationControls();

		// 数据Grid
		Grid<RomFile> grid = new Grid<>(RomFile.class, false);
		VaadinViews.addColumnsTo(grid, RomFile.class);
		grid.setAllRowsVisible(true);

		// DataProvider
		DataProvider<RomFile, Void> dataProvider = DataProvider.fromCallbacks(
				query -> {
					int offset = paginationControls.calculateOffset();
					int limit = paginationControls.getPageSize();
					String name = searchField.getValue();
					return romFileRepo.listByRepo(repoLabel, platformSelect.getValue(),
							name != null && !name.isBlank() ? name : null, offset, limit);
				},
				query -> {
					String name = searchField.getValue();
					int count = romFileRepo.countByRepo(repoLabel, platformSelect.getValue(),
							name != null && !name.isBlank() ? name : null);
					paginationControls.recalculatePageCount(count);
					int offset = paginationControls.calculateOffset();
					int limit = paginationControls.getPageSize();
					int remaining = count - offset;
					return Math.min(remaining, limit);
				});
		grid.setItems(dataProvider);

		// 平台切换或搜索时刷新
		platformSelect.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		searchField.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		paginationControls.onPageChanged(() -> grid.getDataProvider().refreshAll());

		add(header, toolbar, grid, paginationControls);
	}
}
