package io.github.xuse.romaster.ui.export;

import java.io.File;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.support.AutoForm;
import io.github.xuse.jetui.vaadin.support.VaadinForms;
import io.github.xuse.jetui.vaadin.support.VaadinHelper;
import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.obj.GlobalTask;
import io.github.xuse.romking.service.GlobalTaskService;
import io.github.xuse.romking.service.RomExportOptions;
import io.github.xuse.romking.service.RomExportService;
import jakarta.annotation.security.PermitAll;

/**
 * ROM导出视图。
 * 提供导出操作入口和任务历史展示。
 */
@Route("rom-export")
@PageTitle("ROM Export")
@Menu(order = 2, icon = "vaadin:download-alt", title = "ROM Export")
@PermitAll
public class ExportView extends Main {

	private final RomConsole console;
	private final Grid<GlobalTask> taskGrid;

	public ExportView(RomConsole console) {
		this.console = console;

		add(VaadinHelper.viewToolbarBuilder(ExportForm.class)
				.button("导出", this::showExportDialog)
				.build());

		GlobalTaskService taskService = console.getBean(GlobalTaskService.class);
		taskGrid = VaadinViews.createGrid(GlobalTask.class, taskService);

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
		add(taskGrid);
	}

	private void showExportDialog(ClickEvent<Button> event) {
		ExportForm formData = new ExportForm();
		AutoForm<ExportForm> form = VaadinForms.createAutoForm(formData, ExportForm.class);

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("导出ROM到TF卡");
		dialog.setWidth("550px");

		Button closeButton = new Button(new Icon("lumo", "cross"), e -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getHeader().add(closeButton);

		dialog.add(form);

		Button cancelButton = new Button("取消", e -> dialog.close());
		cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getFooter().add(cancelButton);

		Button exportButton = new Button("开始导出", e -> {
			ExportForm data = form.getBeanIfValid();
			if (data != null) {
				doExport(data);
				dialog.close();
			}
		});
		exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dialog.getFooter().add(exportButton);

		dialog.open();
	}

	private void doExport(ExportForm formData) {
		// 校验
		if (formData.getSourceLabel() == null || formData.getSourceLabel().isBlank()) {
			Notification.show("请输入源仓库标签", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}
		if (formData.getTargetPath() == null || formData.getTargetPath().isBlank()) {
			Notification.show("请输入目标TF卡路径", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}

		File targetDir = new File(formData.getTargetPath());
		if (!targetDir.exists() && !targetDir.mkdirs()) {
			Notification.show("目标路径无法创建: " + formData.getTargetPath(), 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}

		try {
			RomExportService exportService = console.getBean(RomExportService.class);

			// 获取源仓库下所有目录ID
			int[] dirIds = exportService.getDirIdsByLabel(formData.getSourceLabel());
			if (dirIds.length == 0) {
				Notification.show("源仓库标签下没有目录: " + formData.getSourceLabel(), 3000, Notification.Position.BOTTOM_END)
						.addThemeVariants(NotificationVariant.LUMO_ERROR);
				return;
			}

			// 构建导出选项
			RomExportOptions options = new RomExportOptions();
			options.setSourceDirIds(dirIds);
			options.setTargetPath(formData.getTargetPath());
			options.setTargetLabel(formData.getTargetLabel());
			options.setOverwrite(formData.isOverwrite());
			options.setQuickExport(formData.isQuickExport());
			options.setMetadataFormat(formData.getMetadataFormat());

			exportService.export(options);
			Notification.show("导出任务已提交", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			taskGrid.getDataProvider().refreshAll();
		} catch (Exception ex) {
			Notification.show("提交失败: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
	}
}
