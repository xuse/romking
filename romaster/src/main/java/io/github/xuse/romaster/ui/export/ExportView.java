package io.github.xuse.romaster.ui.export;

import java.io.File;
import java.util.List;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.support.AutoForm;
import io.github.xuse.jetui.vaadin.support.VaadinForms;
import io.github.xuse.jetui.vaadin.support.VaadinHelper;
import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.ExportPresetRepository;
import io.github.xuse.romking.repo.obj.ExportPreset;
import io.github.xuse.romking.repo.obj.GlobalTask;
import io.github.xuse.romking.service.GlobalTaskService;
import io.github.xuse.romking.service.RomExportOptions;
import io.github.xuse.romking.service.RomExportService;
import jakarta.annotation.security.PermitAll;

/**
 * ROM导出视图。
 * 提供导出操作入口、预设管理和任务历史展示。
 */
@Route("rom-export")
@PageTitle("ROM Export")
@Menu(order = 2, icon = "vaadin:download-alt", title = "ROM Export")
@PermitAll
public class ExportView extends Main {

	private final RomConsole console;
	private final Grid<GlobalTask> taskGrid;
	private final ExportPresetRepository presetRepo;

	public ExportView(RomConsole console) {
		this.console = console;
		this.presetRepo = console.getBean(ExportPresetRepository.class);

		// 工具栏：导出按钮 + 预设选择
		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
		toolbar.setSpacing(true);

		Button exportBtn = new Button("导出", this::showExportDialog);
		exportBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button presetExportBtn = new Button("从预设导出", this::showPresetExportDialog);

		Button savePresetBtn = new Button("保存预设", this::showSavePresetDialog);

		toolbar.add(exportBtn, presetExportBtn, savePresetBtn);
		add(toolbar);

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

	/**
	 * 从预设快速导出
	 */
	private void showPresetExportDialog(ClickEvent<Button> event) {
		List<ExportPreset> presets = presetRepo.listAll();
		if (presets.isEmpty()) {
			Notification.show("暂无保存的预设，请先保存一个导出预设", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
			return;
		}

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("选择导出预设");
		dialog.setWidth("400px");

		Select<ExportPreset> presetSelect = new Select<>();
		presetSelect.setLabel("选择预设");
		presetSelect.setItems(presets);
		presetSelect.setItemLabelGenerator(ExportPreset::getName);
		presetSelect.setWidthFull();
		if (!presets.isEmpty()) {
			presetSelect.setValue(presets.get(0));
		}

		dialog.add(presetSelect);

		Button closeButton = new Button(new Icon("lumo", "cross"), e -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getHeader().add(closeButton);

		Button cancelButton = new Button("取消", e -> dialog.close());
		cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getFooter().add(cancelButton);

		Button exportButton = new Button("开始导出", e -> {
			ExportPreset preset = presetSelect.getValue();
			if (preset != null) {
				doExportFromPreset(preset);
				dialog.close();
			}
		});
		exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dialog.getFooter().add(exportButton);

		dialog.open();
	}

	/**
	 * 保存当前配置为预设
	 */
	private void showSavePresetDialog(ClickEvent<Button> event) {
		ExportForm formData = new ExportForm();
		AutoForm<ExportForm> form = VaadinForms.createAutoForm(formData, ExportForm.class);

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("保存导出预设");
		dialog.setWidth("550px");

		// 预设名称输入
		com.vaadin.flow.component.textfield.TextField presetNameField = new com.vaadin.flow.component.textfield.TextField("预设名称");
		presetNameField.setWidthFull();
		presetNameField.setRequired(true);

		Button closeButton = new Button(new Icon("lumo", "cross"), e -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getHeader().add(closeButton);

		dialog.add(presetNameField, form);

		Button cancelButton = new Button("取消", e -> dialog.close());
		cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getFooter().add(cancelButton);

		Button saveButton = new Button("保存", e -> {
			String presetName = presetNameField.getValue();
			if (presetName == null || presetName.isBlank()) {
				Notification.show("请输入预设名称", 3000, Notification.Position.BOTTOM_END)
						.addThemeVariants(NotificationVariant.LUMO_ERROR);
				return;
			}
			ExportForm data = form.getBeanIfValid();
			if (data != null) {
				savePreset(presetName.trim(), data);
				dialog.close();
			}
		});
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dialog.getFooter().add(saveButton);

		dialog.open();
	}

	private void savePreset(String name, ExportForm formData) {
		ExportPreset preset = new ExportPreset();
		preset.setName(name);
		preset.setSourceLabel(formData.getSourceLabel());
		preset.setTargetPath(formData.getTargetPath());
		preset.setTargetLabel(formData.getTargetLabel());
		preset.setOverwrite(formData.isOverwrite());
		preset.setQuickExport(formData.isQuickExport());
		preset.setMetadataFormat(formData.getMetadataFormat());
		presetRepo.insert(preset);
		Notification.show("预设已保存: " + name, 3000, Notification.Position.BOTTOM_END)
				.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
	}

	private void doExportFromPreset(ExportPreset preset) {
		ExportForm formData = new ExportForm();
		formData.setSourceLabel(preset.getSourceLabel());
		formData.setTargetPath(preset.getTargetPath());
		formData.setTargetLabel(preset.getTargetLabel());
		formData.setOverwrite(preset.isOverwrite());
		formData.setQuickExport(preset.isQuickExport());
		formData.setMetadataFormat(preset.getMetadataFormat());
		doExport(formData);
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
