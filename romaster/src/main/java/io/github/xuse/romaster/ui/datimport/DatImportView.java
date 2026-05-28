package io.github.xuse.romaster.ui.datimport;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.nointro.DatImportConfig;
import io.github.xuse.romking.nointro.DatManageService;
import io.github.xuse.romking.repo.obj.GlobalTask;
import io.github.xuse.romking.service.GlobalTaskService;
import io.github.xuse.romking.tasks.Task;
import io.github.xuse.romaster.ui.support.InlineProgressHelper;
import jakarta.annotation.security.PermitAll;

/**
 * ROM标准数据库导入视图。
 * 导入No-Intro DAT文件到known_rom表，用于ROM版本识别。
 */
@Route("dat-import")
@PageTitle("ROM Database Import")
@PermitAll
public class DatImportView extends Main {

	private final RomConsole console;
	private final Grid<GlobalTask> taskGrid;

	public DatImportView(RomConsole console) {
		this.console = console;

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

		// 统计信息
		DatManageService datService = console.getBean(DatManageService.class);
		long count = datService.getKnownRomCount();
		Span stats = new Span("已导入ROM数据: " + count + " 条");
		stats.getStyle().set("font-size", "var(--lumo-font-size-l)");

		// 平台范围说明
		Span platformInfo = new Span("默认导入平台: " + DatImportConfig.DEFAULT_PLATFORMS);
		platformInfo.getStyle().set("font-size", "var(--lumo-font-size-s)");
		platformInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");

		// 工具栏
		HorizontalLayout toolbar = new HorizontalLayout();
		Button importBtn = new Button("导入DAT目录", this::showImportDialog);
		importBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		toolbar.add(importBtn);

		add(stats, platformInfo, toolbar);

		// 任务列表
		GlobalTaskService taskService = console.getBean(GlobalTaskService.class);
		taskGrid = VaadinViews.createGrid(GlobalTask.class, taskService);
		add(taskGrid);
	}

	private void showImportDialog(ClickEvent<Button> event) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("导入ROM标准数据库（No-Intro DAT）");
		dialog.setWidth("650px");

		Button closeButton = new Button(new Icon("lumo", "cross"), e -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getHeader().add(closeButton);

		// DAT目录路径
		TextField pathField = new TextField("DAT文件目录");
		pathField.setPlaceholder("DAT/ZIP文件所在目录路径");
		pathField.setWidthFull();
		pathField.setValue(getDefaultDatPath());

		// 平台多选（横排3列布局）
		CheckboxGroup<Platform> platformGroup = new CheckboxGroup<>("选择导入平台");
		platformGroup.setItems(Platform.values());
		platformGroup.setValue(DatImportConfig.DEFAULT_PLATFORMS);
		platformGroup.addThemeVariants(CheckboxGroupVariant.LUMO_HELPER_ABOVE_FIELD);
		platformGroup.getStyle().set("display", "grid");
		platformGroup.getStyle().set("grid-template-columns", "repeat(3, 1fr)");
		platformGroup.getStyle().set("gap", "var(--lumo-space-xs)");

		// 全选/取消全选按钮
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
					progress -> taskGrid.getDataProvider().refreshAll(),
					result -> taskGrid.getDataProvider().refreshAll(),
					taskService
			);
			addDetachListener(e -> helper.unsubscribe());
		} catch (Exception ex) {
			Notification.show("提交失败: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
	}

	/**
	 * 获取默认DAT目录路径（项目根目录下的no-intro）
	 */
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
