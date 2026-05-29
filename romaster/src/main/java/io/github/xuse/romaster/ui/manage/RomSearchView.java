package io.github.xuse.romaster.ui.manage;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.component.PaginationControls;
import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.repo.obj.RomFileFilter;
import jakarta.annotation.security.PermitAll;

/**
 * ROM全局搜索视图。
 * 支持按游戏名、ROM文件名、平台组合筛选，跨所有仓库搜索。
 */
@Route("rom-search")
@PageTitle("ROM Search")
@Menu(order = 3, icon = "vaadin:search", title = "ROM搜索")
@PermitAll
public class RomSearchView extends Main {

	private final RomFileRepository romFileRepo;
	private Grid<RomFile> grid;
	private TextField nameField;
	private TextField romNameField;
	private Select<Platform> platformSelect;
	private PaginationControls paginationControls;

	public RomSearchView(RomConsole console) {
		this.romFileRepo = console.getBean(RomFileRepository.class);

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

		buildUI();
	}

	private void buildUI() {
		// 搜索工具栏
		nameField = new TextField();
		nameField.setPlaceholder("游戏名");
		nameField.setAriaLabel("游戏名");
		nameField.setClearButtonVisible(true);
		nameField.setMinWidth("12em");

		romNameField = new TextField();
		romNameField.setPlaceholder("ROM文件名");
		romNameField.setAriaLabel("ROM文件名");
		romNameField.setClearButtonVisible(true);
		romNameField.setMinWidth("12em");

		platformSelect = new Select<>();
		platformSelect.setPlaceholder("全部平台");
		platformSelect.setAriaLabel("平台");
		platformSelect.setEmptySelectionAllowed(true);
		platformSelect.setItems(Platform.values());

		Button searchBtn = new Button("查询", this::doSearch);
		searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button resetBtn = new Button("重置", e -> {
			nameField.clear();
			romNameField.clear();
			platformSelect.clear();
		});

		HorizontalLayout toolbar = new HorizontalLayout(nameField, romNameField, platformSelect, searchBtn, resetBtn);
		toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
		toolbar.setSpacing(true);

		// 分页
		paginationControls = new PaginationControls();

		// 数据Grid
		grid = new Grid<>(RomFile.class, false);
		VaadinViews.addColumnsTo(grid, RomFile.class);
		grid.setAllRowsVisible(true);

		// DataProvider with filter
		DataProvider<RomFile, Void> dataProvider = DataProvider.fromCallbacks(
				query -> {
					int offset = paginationControls.calculateOffset();
					int limit = paginationControls.getPageSize();
					RomFileFilter filter = buildFilter();
					return romFileRepo.list(java.util.Optional.ofNullable(filter), offset, limit);
				},
				query -> {
					RomFileFilter filter = buildFilter();
					int count = romFileRepo.count(java.util.Optional.ofNullable(filter));
					paginationControls.recalculatePageCount(count);
					int offset = paginationControls.calculateOffset();
					int limit = paginationControls.getPageSize();
					int remaining = count - offset;
					return Math.min(remaining, limit);
				});
		grid.setItems(dataProvider);

		paginationControls.onPageChanged(() -> grid.getDataProvider().refreshAll());

		add(toolbar, grid, paginationControls);
	}

	private RomFileFilter buildFilter() {
		RomFileFilter filter = new RomFileFilter();
		String name = nameField.getValue();
		if (name != null && !name.isBlank()) {
			filter.setName(name.trim());
		}
		String romName = romNameField.getValue();
		if (romName != null && !romName.isBlank()) {
			filter.setRomName(romName.trim());
		}
		Platform platform = platformSelect.getValue();
		if (platform != null) {
			filter.setPlatform(platform);
		}
		// 如果所有条件都为空，返回null表示无过滤
		if (filter.getName() == null && filter.getRomName() == null && filter.getPlatform() == null) {
			return null;
		}
		return filter;
	}

	private void doSearch(ClickEvent<Button> event) {
		grid.getDataProvider().refreshAll();
	}
}
