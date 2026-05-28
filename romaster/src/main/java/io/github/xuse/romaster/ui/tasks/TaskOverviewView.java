package io.github.xuse.romaster.ui.tasks;

import java.io.File;
import java.time.Duration;
import java.util.List;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.support.AutoForm;
import io.github.xuse.jetui.vaadin.support.VaadinForms;
import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.obj.GlobalTask;
import io.github.xuse.romking.service.GlobalTaskService;
import io.github.xuse.romking.service.RomImportService;
import io.github.xuse.romking.service.RomScanOptions;
import io.github.xuse.romking.tasks.ProcessResult;
import io.github.xuse.romking.tasks.Task;
import io.github.xuse.romking.tasks.TaskProgress;
import io.github.xuse.romking.tasks.TaskProgressListener;
import io.github.xuse.romking.tasks.TaskType;
import io.github.xuse.romaster.ui.scan.TaskForm;
import jakarta.annotation.security.PermitAll;

@Route("tasks")
@PageTitle("任务总览")
@Menu(order = 1, icon = "vaadin:tasks", title = "任务总览")
@PermitAll
public class TaskOverviewView extends Main implements TaskProgressListener {

	private final RomConsole console;
	private final GlobalTaskService taskService;
	private final VerticalLayout activeSection;
	private final Grid<GlobalTask> historyGrid;
	private UI ui;

	public TaskOverviewView(RomConsole console) {
		this.console = console;
		this.taskService = console.getBean(GlobalTaskService.class);

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

		// Toolbar with scan button
		Button scanBtn = new Button("扫描ROM目录", new Icon("vaadin", "search"), this::scanDialog);
		scanBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout toolbar = new HorizontalLayout(scanBtn);
		toolbar.setWidthFull();

		// Active tasks section
		Span activeTitle = new Span("活动任务");
		activeTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
		activeTitle.getStyle().set("font-weight", "bold");

		activeSection = new VerticalLayout();
		activeSection.setPadding(false);
		activeSection.setSpacing(true);

		// History section
		Span historyTitle = new Span("历史任务");
		historyTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
		historyTitle.getStyle().set("font-weight", "bold");
		historyTitle.getStyle().set("margin-top", "var(--lumo-space-l)");

		historyGrid = buildHistoryGrid();

		add(toolbar, activeTitle, activeSection, historyTitle, historyGrid);
	}

	@Override
	protected void onAttach(AttachEvent event) {
		super.onAttach(event);
		this.ui = event.getUI();
		taskService.addListener(this);
		refreshActiveSection();
	}

	@Override
	protected void onDetach(DetachEvent event) {
		taskService.removeListener(this);
		super.onDetach(event);
	}

	@Override
	public void onProgressChanged(Task task, TaskProgress progress) {
		ui.access(() -> refreshActiveSection());
	}

	@Override
	public void onTaskCompleted(Task task, ProcessResult result) {
		ui.access(() -> {
			refreshActiveSection();
			historyGrid.getDataProvider().refreshAll();
		});
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
		RomScanOptions options = new RomScanOptions();
		options.setLabel(formData.getLabel());
		options.setPlatform(formData.getPlatform());
		options.setScanWithoutGamelist(formData.isScanWithoutGamelist());
		options.setComputeMd5(formData.isComputeMd5());
		options.setComputeCrc(formData.isComputeCrc());
		options.setIncremental(formData.isIncremental());
		try {
			RomImportService importService = console.getBean(RomImportService.class);
			importService.scan(dir, options);
			Notification.show("扫描任务已提交", 3000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			refreshActiveSection();
		} catch (Exception ex) {
			Notification.show("提交失败: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_END)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
	}

	private void refreshActiveSection() {
		activeSection.removeAll();
		List<Task> activeTasks = taskService.getActiveTasks();
		if (activeTasks.isEmpty()) {
			Span empty = new Span("当前没有运行中的任务");
			empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
			activeSection.add(empty);
			return;
		}
		for (Task task : activeTasks) {
			activeSection.add(buildTaskCard(task));
		}
	}

	private HorizontalLayout buildTaskCard(Task task) {
		HorizontalLayout card = new HorizontalLayout();
		card.setWidthFull();
		card.setAlignItems(HorizontalLayout.Alignment.CENTER);
		card.getStyle().set("padding", "var(--lumo-space-s)");
		card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
		card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

		// Task type icon
		Icon icon = getTaskTypeIcon(task.getType());
		icon.setSize("24px");

		// Task name
		Span name = new Span(task.getName());
		name.getStyle().set("font-weight", "500");
		name.getStyle().set("min-width", "150px");

		// Progress info
		TaskProgress progress = task.getTaskProgress();
		VerticalLayout progressLayout = new VerticalLayout();
		progressLayout.setPadding(false);
		progressLayout.setSpacing(false);
		progressLayout.setWidthFull();

		// Current step text
		String stepText = progress != null ? progress.getCurrentStep() : "";
		Span stepSpan = new Span(stepText);
		stepSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
		stepSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

		// Progress bar
		ProgressBar progressBar = new ProgressBar();
		progressBar.setMin(0);
		progressBar.setMax(1.0);
		double pct = progress != null ? progress.getPercentage() / 100.0 : 0;
		progressBar.setValue(pct);

		progressLayout.add(stepSpan, progressBar);

		// Elapsed time
		long elapsed = System.currentTimeMillis() - task.getBegin();
		String elapsedText = formatElapsed(elapsed);
		Span elapsedSpan = new Span(elapsedText);
		elapsedSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
		elapsedSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
		elapsedSpan.getStyle().set("min-width", "80px");
		elapsedSpan.getStyle().set("text-align", "right");

		card.add(icon, name, progressLayout, elapsedSpan);
		card.expand(progressLayout);
		return card;
	}

	private Grid<GlobalTask> buildHistoryGrid() {
		Grid<GlobalTask> grid = VaadinViews.createGrid(GlobalTask.class, taskService);
		grid.setPageSize(20);
		return grid;
	}

	private static Icon getTaskTypeIcon(TaskType type) {
		if (type == null) {
			return new Icon("vaadin", "tasks");
		}
		switch (type) {
		case SCAN_DIR:
			return new Icon("vaadin", "search");
		case EXPORT:
			return new Icon("vaadin", "download");
		case ARCHIVE:
			return new Icon("vaadin", "archive");
		case VERIFY:
			return new Icon("vaadin", "check-circle");
		case IMPORT_DAT:
			return new Icon("vaadin", "database");
		default:
			return new Icon("vaadin", "tasks");
		}
		}

	private static String formatElapsed(long millis) {
		Duration d = Duration.ofMillis(millis);
		long hours = d.toHours();
		long minutes = d.toMinutesPart();
		long seconds = d.toSecondsPart();
		if (hours > 0) {
			return String.format("%dh%02dm%02ds", hours, minutes, seconds);
		} else if (minutes > 0) {
			return String.format("%dm%02ds", minutes, seconds);
		} else {
			return String.format("%ds", seconds);
		}
	}
}
