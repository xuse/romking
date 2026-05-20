package io.github.xuse.romaster.ui.scan;

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
import io.github.xuse.romking.service.RomImportService;
import io.github.xuse.romking.service.RomScanOptions;
import jakarta.annotation.security.PermitAll;

@Route("task-list")
@PageTitle("Task List")
@Menu(order = 1, icon = "vaadin:clipboard-check", title = "Global Tasks")
@PermitAll
public class GlobalTaskView extends Main {

	final Grid<GlobalTask> taskGrid;
	final RomConsole console;

	public GlobalTaskView(RomConsole console) {
		this.console = console;

		add(VaadinHelper.viewToolbarBuilder(TaskForm.class)
				.button("扫描", this::scanDialog)
				.build());
		GlobalTaskService taskService = console.getBean(GlobalTaskService.class);
		add(taskGrid = VaadinViews.createGrid(GlobalTask.class, taskService));
		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
	}

	private void scanDialog(ClickEvent<Button> event) {
		TaskForm formData = new TaskForm();
		AutoForm<TaskForm> form = VaadinForms.createAutoForm(formData, TaskForm.class);

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("扫描ROM目录");
		dialog.setWidth("500px");

		Button closeButton = new Button(new Icon("lumo", "cross"), (e) -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getHeader().add(closeButton);

		dialog.add(form);

		Button cancelButton = new Button("取消", (e) -> dialog.close());
		cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getFooter().add(cancelButton);

		Button scanButton = new Button("开始扫描", (e) -> {
			TaskForm data = form.getBeanIfValid();
			if (data != null) {
				doScan(data);
				dialog.close();
			}
		});
		scanButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dialog.getFooter().add(scanButton);

		dialog.open();
	}

	private void doScan(TaskForm formData) {
		// 校验
		if (formData.getPath() == null || formData.getPath().isBlank()) {
			Notification.show("请输入扫描路径", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}
		if (formData.getLabel() == null || formData.getLabel().isBlank()) {
			Notification.show("请输入仓库标签", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}

		File dir = new File(formData.getPath());
		if (!dir.isDirectory()) {
			Notification.show("路径不存在或不是目录: " + formData.getPath(), 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}

		// 构建扫描选项
		RomScanOptions options = new RomScanOptions();
		options.setLabel(formData.getLabel());
		options.setPlatform(formData.getPlatform());
		options.setScanWithoutGamelist(formData.isScanWithoutGamelist());
		options.setComputeMd5(formData.isComputeMd5());
		options.setComputeCrc(formData.isComputeCrc());

		// 提交扫描任务
		try {
			RomImportService importService = console.getBean(RomImportService.class);
			importService.scan(dir, options);
			Notification.show("扫描任务已提交", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			// 刷新任务列表
			taskGrid.getDataProvider().refreshAll();
		} catch (Exception ex) {
			Notification.show("提交失败: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
	}
}
