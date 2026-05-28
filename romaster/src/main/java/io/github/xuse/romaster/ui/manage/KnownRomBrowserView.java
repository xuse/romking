package io.github.xuse.romaster.ui.manage;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.component.PaginationControls;
import io.github.xuse.jetui.vaadin.component.ViewToolbar;
import io.github.xuse.jetui.vaadin.support.FormatUtils;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.nointro.DatImportConfig;
import io.github.xuse.romking.nointro.DatManageService;
import io.github.xuse.romking.repo.dal.KnownRomRepository;
import io.github.xuse.romking.repo.enums.Region;
import io.github.xuse.romking.repo.obj.KnownRom;
import io.github.xuse.romking.repo.obj.KnownRomFilter;
import io.github.xuse.romking.service.GlobalTaskService;
import io.github.xuse.romking.tasks.Task;
import io.github.xuse.romaster.ui.support.InlineProgressHelper;
import jakarta.annotation.security.PermitAll;

/**
 * 标准ROM库浏览视图。
 * 支持按平台、区域、游戏名组合筛选，分页浏览已导入的ROM标准数据。
 * 同一parentName的多个ROM以展开/折叠方式显示。
 */
@Route("known-roms")
@PageTitle("标准ROM库")
@Menu(order = 4, icon = "vaadin:database", title = "标准ROM库")
@PermitAll
public class KnownRomBrowserView extends Main {

	private final RomConsole console;
	private final KnownRomRepository knownRomRepo;
	private final KnownRomFilter filter = new KnownRomFilter();
	private final PaginationControls paginationControls = new PaginationControls();
	private Grid<KnownRom> grid;
	private ConfigurableFilterDataProvider<KnownRom, Void, KnownRomFilter> filterDataProvider;

	// 工具栏中的输入控件引用
	private Select<Platform> platformSelect;
	private Select<Region> regionSelect;
	private TextField gameNameField;

	public KnownRomBrowserView(RomConsole console) {
		this.console = console;
		this.knownRomRepo = console.getBean(KnownRomRepository.class);

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
		setSizeFull();

		add(createStatisticsPanel());
		add(createFilterToolbar());
		createDataGrid();
		add(grid);
		add(paginationControls);
	}

	/**
	 * 创建统计信息面板（含导入按钮）
	 */
	private Component createStatisticsPanel() {
		long total = knownRomRepo.count(Optional.empty());

		HorizontalLayout layout = new HorizontalLayout();
		layout.setWidthFull();
		layout.setAlignItems(HorizontalLayout.Alignment.CENTER);

		if (total == 0) {
			layout.add(new Span("暂无数据，请点击\"导入DAT\"按钮导入标准ROM数据"));
		} else {
			Map<Platform, Long> platformCounts = knownRomRepo.countByPlatform();
			layout.add(new Span("总记录数: " + total));
			StringBuilder sb = new StringBuilder();
			platformCounts.forEach((p, c) -> sb.append(p.name()).append(": ").append(c).append("  "));
			layout.add(new Span(sb.toString().trim()));
		}

		Button importBtn = new Button("导入DAT", new Icon("vaadin", "upload"), this::showImportDialog);
		importBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
		layout.add(importBtn);
		layout.expand(layout.getComponentAt(0));

		return layout;
	}

	/**
	 * 创建筛选工具栏
	 */
	private Component createFilterToolbar() {
		platformSelect = new Select<>();
		platformSelect.setPlaceholder("平台");
		platformSelect.setAriaLabel("平台");
		platformSelect.setItems(Platform.values());
		platformSelect.setEmptySelectionAllowed(true);

		regionSelect = new Select<>();
		regionSelect.setPlaceholder("区域");
		regionSelect.setAriaLabel("区域");
		regionSelect.setItems(Region.values());
		regionSelect.setEmptySelectionAllowed(true);

		gameNameField = new TextField();
		gameNameField.setPlaceholder("输入游戏名关键字");
		gameNameField.setAriaLabel("游戏名");
		gameNameField.setMinWidth("20em");

		Button searchBtn = new Button("查询", this::doSearch);
		searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button resetBtn = new Button("重置", this::doReset);

		return new ViewToolbar("标准ROM库",
				ViewToolbar.group(platformSelect, regionSelect, gameNameField, searchBtn, resetBtn));
	}

	/**
	 * 创建数据Grid（带分页 + 展开折叠）
	 */
	private void createDataGrid() {
		DataProvider<KnownRom, KnownRomFilter> dataProvider = DataProvider.fromFilteringCallbacks(query -> {
			var offset = paginationControls.calculateOffset();
			var limit = paginationControls.getPageSize();
			return knownRomRepo.list(query.getFilter(), offset, limit);
		}, query -> {
			var itemCount = knownRomRepo.count(query.getFilter());
			paginationControls.recalculatePageCount(itemCount);
			var offset = paginationControls.calculateOffset();
			var limit = paginationControls.getPageSize();
			var remaining = itemCount - offset;
			return Math.min(remaining, limit);
		});
		filterDataProvider = dataProvider.withConfigurableFilter();

		grid = new Grid<>(KnownRom.class, false);
		grid.setItems(filterDataProvider);

		// 展开/折叠按钮列
		grid.addColumn(createToggleDetailsRenderer(grid))
				.setWidth("60px").setFlexGrow(0).setFrozen(true);

		// 游戏名 - 加宽
		grid.addColumn(KnownRom::getGameName)
				.setHeader("游戏名").setKey("gameName")
				.setFlexGrow(3).setSortable(true);

		// 平台 - 缩短
		grid.addColumn(rom -> rom.getPlatform() != null ? rom.getPlatform().name() : "")
				.setHeader("平台").setKey("platform")
				.setWidth("70px").setFlexGrow(0).setSortable(true);

		// 区域 - 缩短
		grid.addColumn(rom -> rom.getRegion() != null ? rom.getRegion().name() : "")
				.setHeader("区域").setKey("region")
				.setWidth("60px").setFlexGrow(0);

		// 文件大小
		grid.addColumn(rom -> FormatUtils.formatFileSize(rom.getRomSize()))
				.setHeader("大小").setKey("romSize")
				.setWidth("80px").setFlexGrow(0);

		// CRC
		grid.addColumn(KnownRom::getCrc)
				.setHeader("CRC").setKey("crc")
				.setWidth("90px").setFlexGrow(0);

		// MD5
		grid.addColumn(KnownRom::getMd5)
				.setHeader("MD5").setKey("md5")
				.setFlexGrow(1);

		// 配置展开详情（同parentName的其他ROM）
		grid.setDetailsVisibleOnClick(false);
		grid.setItemDetailsRenderer(createRomDetailsRenderer());
		grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
		grid.setAllRowsVisible(true);

		paginationControls.onPageChanged(() -> grid.getDataProvider().refreshAll());
	}

	/**
	 * 创建展开/折叠按钮渲染器
	 */
	private static Renderer<KnownRom> createToggleDetailsRenderer(Grid<KnownRom> grid) {
		return LitRenderer.<KnownRom>of("""
				<vaadin-button
					theme="tertiary icon"
					aria-label="Toggle details"
					aria-expanded="${model.detailsOpened ? 'true' : 'false'}"
					@click="${handleClick}"
				>
					<vaadin-icon .icon="${model.detailsOpened ? 'lumo:angle-down' : 'lumo:angle-right'}"></vaadin-icon>
				</vaadin-button>
				""")
				.withFunction("handleClick",
						rom -> grid.setDetailsVisible(rom, !grid.isDetailsVisible(rom)));
	}

	/**
	 * 创建详情渲染器：展示同parentName下的所有ROM变体
	 */
	private ComponentRenderer<Component, KnownRom> createRomDetailsRenderer() {
		return new ComponentRenderer<>(rom -> {
			String parent = rom.getParentName();
			if (parent == null || parent.isEmpty()) {
				parent = rom.getGameName();
			}
			List<KnownRom> siblings = knownRomRepo.findByParentName(parent, rom.getPlatform());

			// 过滤掉当前行自身，只显示其他变体
			siblings.removeIf(r -> r.getId() == rom.getId());

			if (siblings.isEmpty()) {
				Span noVariants = new Span("无其他版本");
				noVariants.getStyle().set("color", "var(--lumo-secondary-text-color)");
				noVariants.getStyle().set("padding", "var(--lumo-space-s)");
				return noVariants;
			}

			// 用内嵌Grid展示变体列表
			Grid<KnownRom> detailGrid = new Grid<>(KnownRom.class, false);
			detailGrid.addColumn(KnownRom::getGameName)
					.setHeader("游戏名").setFlexGrow(3);
			detailGrid.addColumn(r -> r.getRegion() != null ? r.getRegion().name() : "")
					.setHeader("区域").setWidth("60px").setFlexGrow(0);
			detailGrid.addColumn(r -> FormatUtils.formatFileSize(r.getRomSize()))
					.setHeader("大小").setWidth("80px").setFlexGrow(0);
			detailGrid.addColumn(KnownRom::getCrc)
					.setHeader("CRC").setWidth("90px").setFlexGrow(0);
			detailGrid.addColumn(KnownRom::getMd5)
					.setHeader("MD5").setFlexGrow(1);
			detailGrid.setItems(siblings);
			detailGrid.setAllRowsVisible(true);
			detailGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER);
			detailGrid.getStyle().set("padding-left", "60px");

			return detailGrid;
		});
	}

	// ========== 查询/重置 ==========

	private void doSearch(ClickEvent<Button> event) {
		readFilterFromUI();
		filterDataProvider.setFilter(filter);
	}

	private void doReset(ClickEvent<Button> event) {
		filter.setPlatform(null);
		filter.setRegion(null);
		filter.setGameName(null);
		updateFilterUI();
		filterDataProvider.setFilter(null);
	}

	private void readFilterFromUI() {
		filter.setPlatform(platformSelect.getValue());
		filter.setRegion(regionSelect.getValue());
		String text = gameNameField.getValue();
		filter.setGameName(text != null && !text.isBlank() ? text : null);
	}

	private void updateFilterUI() {
		platformSelect.setValue(filter.getPlatform());
		regionSelect.setValue(filter.getRegion());
		gameNameField.setValue(filter.getGameName() != null ? filter.getGameName() : "");
	}

	// ========== DAT导入功能 ==========

	private void showImportDialog(ClickEvent<Button> event) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("导入ROM标准数据库（No-Intro DAT）");
		dialog.setWidth("650px");

		Button closeButton = new Button(new Icon("lumo", "cross"), e -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getHeader().add(closeButton);

		TextField pathField = new TextField("DAT文件目录");
		pathField.setPlaceholder("DAT/ZIP文件所在目录路径");
		pathField.setWidthFull();
		pathField.setValue(getDefaultDatPath());

		CheckboxGroup<Platform> platformGroup = new CheckboxGroup<>("选择导入平台");
		platformGroup.setItems(Platform.values());
		platformGroup.setValue(DatImportConfig.DEFAULT_PLATFORMS);
		platformGroup.addThemeVariants(CheckboxGroupVariant.LUMO_HELPER_ABOVE_FIELD);
		platformGroup.getStyle().set("display", "grid");
		platformGroup.getStyle().set("grid-template-columns", "repeat(3, 1fr)");
		platformGroup.getStyle().set("gap", "var(--lumo-space-xs)");

		HorizontalLayout selectButtons = new HorizontalLayout();
		Button selectAll = new Button("全选", e -> platformGroup.setValue(EnumSet.allOf(Platform.class)));
		selectAll.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
		Button selectNone = new Button("取消全选", e -> platformGroup.deselectAll());
		selectNone.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
		Button selectDefault = new Button("默认", e -> platformGroup.setValue(DatImportConfig.DEFAULT_PLATFORMS));
		selectDefault.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
		selectButtons.add(selectAll, selectNone, selectDefault);

		Span hint = new Span("增量导入：已导入过的平台+版本会自动跳过");
		hint.getStyle().set("font-size", "var(--lumo-font-size-xs)");
		hint.getStyle().set("color", "var(--lumo-secondary-text-color)");

		VerticalLayout content = new VerticalLayout(pathField, selectButtons, platformGroup, hint);
		content.setPadding(false);
		content.setSpacing(true);
		dialog.add(content);

		Button cancelButton = new Button("取消", e -> dialog.close());
		cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getFooter().add(cancelButton);

		Button startButton = new Button("开始导入", e -> {
			String path = pathField.getValue();
			Set<Platform> selected = platformGroup.getValue();
			if (path == null || path.isBlank()) {
				Notification.show("请输入DAT文件目录路径", 3000, Notification.Position.BOTTOM_END)
						.addThemeVariants(NotificationVariant.LUMO_ERROR);
				return;
			}
			if (selected == null || selected.isEmpty()) {
				Notification.show("请至少选择一个平台", 3000, Notification.Position.BOTTOM_END)
						.addThemeVariants(NotificationVariant.LUMO_ERROR);
				return;
			}
			doImport(path, selected);
			dialog.close();
		});
		startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dialog.getFooter().add(startButton);

		dialog.open();
	}

	private void doImport(String path, Set<Platform> platforms) {
		File dir = new File(path);
		if (!dir.exists()) {
			Notification.show("目录不存在: " + path, 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}
		try {
			DatManageService datService = console.getBean(DatManageService.class);
			Task importTask = datService.submitImport(path, null, false, platforms);
			Notification.show("导入任务已提交（" + platforms.size() + "个平台）",
					3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

			GlobalTaskService taskService = console.getBean(GlobalTaskService.class);
			InlineProgressHelper helper = new InlineProgressHelper(
					importTask, getUI().orElseThrow(),
					progress -> grid.getDataProvider().refreshAll(),
					result -> grid.getDataProvider().refreshAll(),
					taskService);
			addDetachListener(e -> helper.unsubscribe());
		} catch (Exception ex) {
			Notification.show("提交失败: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
	}

	private String getDefaultDatPath() {
		File candidate = new File("no-intro");
		if (candidate.isDirectory()) {
			return candidate.getAbsolutePath();
		}
		candidate = new File("../no-intro");
		if (candidate.isDirectory()) {
			return candidate.getAbsolutePath();
		}
		return "";
	}
}
