package io.github.xuse.romaster.ui.archive;

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
import io.github.xuse.romking.service.RomArchiveService;
import jakarta.annotation.security.PermitAll;

/**
 * ROM归档视图。
 * 将INSTANCE仓库中的ROM归档到ARCHIVE仓库（基于MD5去重，含文件复制）。
 */
@Route("rom-archive")
@PageTitle("ROM Archive")
@Menu(order = 4, icon = "vaadin:archive", title = "ROM Archive")
@PermitAll
public class ArchiveView extends Main {

	private final RomConsole console;
	private final Grid<GlobalTask> taskGrid;

	public ArchiveView(RomConsole console) {
		this.console = console;

		add(VaadinHelper.viewToolbarBuilder(ArchiveForm.class)
				.button("归档", this::showArchiveDialog)
				.build());

		GlobalTaskService taskService = console.getBean(GlobalTaskService.class);
		taskGrid = VaadinViews.createGrid(GlobalTask.class, taskService);

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
		add(taskGrid);
	}

	private void showArchiveDialog(ClickEvent<Button> event) {
		ArchiveForm formData = new ArchiveForm();
		AutoForm<ArchiveForm> form = VaadinForms.createAutoForm(formData, ArchiveForm.class);

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("ROM归档（INSTANCE → ARCHIVE）");
		dialog.setWidth("500px");

		Button closeButton = new Button(new Icon("lumo", "cross"), e -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getHeader().add(closeButton);

		dialog.add(form);

		Button cancelButton = new Button("取消", e -> dialog.close());
		cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getFooter().add(cancelButton);

		Button archiveButton = new Button("开始归档", e -> {
			ArchiveForm data = form.getBeanIfValid();
			if (data != null) {
				doArchive(data);
				dialog.close();
			}
		});
		archiveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dialog.getFooter().add(archiveButton);

		dialog.open();
	}

	private void doArchive(ArchiveForm formData) {
		if (formData.getSourceLabel() == null || formData.getSourceLabel().isBlank()) {
			Notification.show("请输入源仓库label", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}
		if (formData.getTargetLabel() == null || formData.getTargetLabel().isBlank()) {
			Notification.show("请输入目标仓库label", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}

		try {
			RomArchiveService archiveService = console.getBean(RomArchiveService.class);
			archiveService.archiveByLabel(formData.getSourceLabel(), formData.getTargetLabel());
			Notification.show("归档任务已提交", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			taskGrid.getDataProvider().refreshAll();
		} catch (Exception ex) {
			Notification.show("提交失败: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
	}
}
