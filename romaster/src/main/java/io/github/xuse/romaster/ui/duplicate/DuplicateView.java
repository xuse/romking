package io.github.xuse.romaster.ui.duplicate;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.repo.vo.DuplicateGroup;
import io.github.xuse.romking.repo.vo.DuplicateGroup.DuplicateType;
import io.github.xuse.romking.service.RomDuplicateService;
import jakarta.annotation.security.PermitAll;

/**
 * ROM重复检测与版本管理视图。
 * 
 * 两个Tab页：
 * 1. 完全重复（同MD5）：冗余副本，可安全删除多余的
 * 2. 同游戏多版本（同gameid）：不同版本，用户选择保留哪些
 */
@Route("rom-duplicates")
@PageTitle("ROM Duplicates")
@Menu(order = 6, icon = "vaadin:copy-o", title = "重复检测")
@PermitAll
public class DuplicateView extends Main {

	private final RomConsole console;
	private final RomDuplicateService duplicateService;

	private Grid<DuplicateGroup> exactGrid;
	private Grid<DuplicateGroup> versionGrid;
	private VerticalLayout exactLayout;
	private VerticalLayout versionLayout;

	public DuplicateView(RomConsole console) {
		this.console = console;
		this.duplicateService = console.getBean(RomDuplicateService.class);

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

		buildUI();
	}

	private void buildUI() {
		// 工具栏
		HorizontalLayout toolbar = new HorizontalLayout();
		Button refreshBtn = new Button("刷新检测", e -> refreshData());
		refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button batchCleanBtn = new Button("批量清理重复", e -> batchCleanDuplicates());
		batchCleanBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
		toolbar.add(refreshBtn, batchCleanBtn);
		add(toolbar);

		// Tab页
		Tab exactTab = new Tab("完全重复（同MD5）");
		Tab versionTab = new Tab("同游戏多版本");
		Tabs tabs = new Tabs(exactTab, versionTab);

		// 完全重复Grid
		exactGrid = new Grid<>();
		exactGrid.addColumn(DuplicateGroup::getGameName).setHeader("游戏名").setFlexGrow(3);
		exactGrid.addColumn(DuplicateGroup::getPlatform).setHeader("平台").setWidth("80px");
		exactGrid.addColumn(DuplicateGroup::getCount).setHeader("副本数").setWidth("80px");
		exactGrid.addColumn(g -> g.getGroupKey() != null ? g.getGroupKey().substring(0, Math.min(8, g.getGroupKey().length())) + "..." : "")
				.setHeader("MD5").setWidth("120px");
		exactGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
		exactGrid.addItemClickListener(e -> showGroupDetail(e.getItem()));
		exactGrid.setSizeFull();

		exactLayout = new VerticalLayout(exactGrid);
		exactLayout.setPadding(false);
		exactLayout.setSizeFull();

		// 同游戏多版本Grid
		versionGrid = new Grid<>();
		versionGrid.addColumn(DuplicateGroup::getGameName).setHeader("游戏名").setFlexGrow(3);
		versionGrid.addColumn(DuplicateGroup::getPlatform).setHeader("平台").setWidth("80px");
		versionGrid.addColumn(DuplicateGroup::getCount).setHeader("版本数").setWidth("80px");
		versionGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
		versionGrid.addItemClickListener(e -> showGroupDetail(e.getItem()));
		versionGrid.setSizeFull();

		versionLayout = new VerticalLayout(versionGrid);
		versionLayout.setPadding(false);
		versionLayout.setSizeFull();
		versionLayout.setVisible(false);

		// Tab切换
		tabs.addSelectedChangeListener(event -> {
			boolean isExact = event.getSelectedTab().equals(exactTab);
			exactLayout.setVisible(isExact);
			versionLayout.setVisible(!isExact);
		});

		add(tabs, exactLayout, versionLayout);

		// 初始加载数据
		refreshData();
	}

	private void refreshData() {
		List<DuplicateGroup> exactDups = duplicateService.findExactDuplicates(0);
		exactGrid.setItems(exactDups);

		List<DuplicateGroup> versions = duplicateService.findGameVersions(0);
		versionGrid.setItems(versions);
	}

	/**
	 * 批量清理完全重复的ROM记录。
	 * 对每组重复（同MD5），保留第一条记录，删除其余记录。
	 * 仅删除数据库记录，不删除物理文件。
	 */
	private void batchCleanDuplicates() {
		List<DuplicateGroup> exactDups = duplicateService.findExactDuplicates(0);
		if (exactDups.isEmpty()) {
			Notification.show("没有发现完全重复的ROM", 3000, Notification.Position.BOTTOM_END);
			return;
		}

		ConfirmDialog confirm = new ConfirmDialog();
		confirm.setHeader("批量清理确认");
		confirm.setText(String.format("将对 %d 组完全重复的ROM执行清理：每组保留1条记录，删除其余冗余记录。\n" +
				"仅删除数据库记录，不删除物理文件。\n确认继续？", exactDups.size()));
		confirm.setCancelable(true);
		confirm.setConfirmText("确认清理");
		confirm.setConfirmButtonTheme("error primary");
		confirm.addConfirmListener(ev -> {
			int totalDeleted = duplicateService.batchCleanExactDuplicates();
			Notification.show("批量清理完成，共删除 " + totalDeleted + " 条冗余记录",
					5000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			refreshData();
		});
		confirm.open();
	}

	/**
	 * 显示分组详情对话框
	 */
	private void showGroupDetail(DuplicateGroup group) {
		List<RomFile> members;
		if (group.getType() == DuplicateType.SAME_MD5) {
			members = duplicateService.getGroupMembers(group.getGroupKey(), group.getType());
		} else {
			members = duplicateService.getVersionsByGameId(group.getGroupKey());
		}

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(group.getGameName() + " - " +
				(group.getType() == DuplicateType.SAME_MD5 ? "重复副本" : "多版本"));
		dialog.setWidth("800px");
		dialog.setHeight("500px");

		Button closeButton = new Button(new Icon("lumo", "cross"), e -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getHeader().add(closeButton);

		// 详情Grid
		Grid<RomFile> detailGrid = new Grid<>();
		detailGrid.addColumn(rom -> rom.getDisplayName() != null ? rom.getDisplayName() : rom.getName())
				.setHeader("游戏名").setFlexGrow(2);
		detailGrid.addColumn(RomFile::getFilepath).setHeader("文件路径").setFlexGrow(3);
		detailGrid.addColumn(RomFile::getRegion).setHeader("区域").setWidth("60px");
		detailGrid.addColumn(rom -> rom.getMd5() != null ? rom.getMd5().substring(0, 8) + "..." : "")
				.setHeader("MD5").setWidth("100px");
		detailGrid.addColumn(RomFile::getDirId).setHeader("仓库ID").setWidth("70px");

		// 删除按钮列
		detailGrid.addComponentColumn(rom -> {
			Button delBtn = new Button("删除记录");
			delBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
			delBtn.addClickListener(e -> {
				ConfirmDialog confirm = new ConfirmDialog();
				confirm.setHeader("确认删除");
				confirm.setText("删除数据库记录（不删除物理文件）：\n" + rom.getFilepath());
				confirm.setCancelable(true);
				confirm.setConfirmText("删除");
				confirm.setConfirmButtonTheme("error primary");
				confirm.addConfirmListener(ev -> {
					duplicateService.deleteRecord(rom.getId());
					members.remove(rom);
					detailGrid.setItems(members);
					Notification.show("已删除记录", 2000, Notification.Position.BOTTOM_END)
							.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				});
				confirm.open();
			});
			return delBtn;
		}).setHeader("操作").setWidth("100px");

		detailGrid.setItems(members);
		detailGrid.setSizeFull();
		detailGrid.addThemeVariants(GridVariant.LUMO_COMPACT);

		dialog.add(detailGrid);
		dialog.open();
	}
}
