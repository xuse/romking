package io.github.xuse.romaster.ui.manage;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.support.AutoForm;
import io.github.xuse.jetui.vaadin.support.VaadinForms;
import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.obj.MediaFile;
import io.github.xuse.romking.repo.obj.MediaFileFilter;
import io.github.xuse.romking.repo.obj.QRomFile;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.repo.obj.RomFileFilter;
import io.github.xuse.romaster.ui.manage.RomEditForm;
import jakarta.annotation.security.PermitAll;

/**
 * ROM文件列表视图，按目录ID展示ROM文件和媒体文件（Tab页切换）。
 * 支持Favorite标注和点击编辑ROM信息。
 */
@Route("rom-files")
@PageTitle("ROM Files")
@PermitAll
public class RomFileListView extends Main implements HasUrlParameter<Integer> {

	private final RomConsole console;
	private final RomFileRepository romFileRepo;
	private final MediaFileRepository mediaFileRepo;

	private Grid<RomFile> romGrid;
	private Grid<MediaFile> mediaGrid;
	private VerticalLayout romLayout;
	private VerticalLayout mediaLayout;
	private int dirId;

	public RomFileListView(RomConsole console) {
		this.console = console;
		this.romFileRepo = console.getBean(RomFileRepository.class);
		this.mediaFileRepo = console.getBean(MediaFileRepository.class);

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
	}

	@Override
	public void setParameter(BeforeEvent event, Integer parameter) {
		this.dirId = parameter != null ? parameter : 0;
		buildUI();
	}

	private void buildUI() {
		removeAll();

		// Tab页
		Tab romTab = new Tab("ROM文件");
		Tab mediaTab = new Tab("媒体文件");
		Tabs tabs = new Tabs(romTab, mediaTab);

		// ROM文件Grid
		romGrid = VaadinViews.createGrid(RomFile.class, romFileRepo);
		romGrid.setSizeFull();

		// 添加Favorite操作列
		romGrid.addComponentColumn(romFile -> {
			String star = romFile.getFavorite() > 0 ? "★" : "☆";
			Button btn = new Button(star);
			btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			btn.addClickListener(e -> toggleFavorite(romFile, btn));
			return btn;
		}).setHeader("收藏").setWidth("60px").setFlexGrow(0);

		// 点击行打开编辑对话框
		romGrid.addItemClickListener(event -> openEditDialog(event.getItem()));

		romLayout = new VerticalLayout(romGrid);
		romLayout.setPadding(false);
		romLayout.setSizeFull();

		// 媒体文件Grid
		mediaGrid = VaadinViews.createGrid(MediaFile.class, mediaFileRepo);
		mediaGrid.setSizeFull();
		mediaLayout = new VerticalLayout(mediaGrid);
		mediaLayout.setPadding(false);
		mediaLayout.setSizeFull();
		mediaLayout.setVisible(false);

		// Tab切换逻辑
		tabs.addSelectedChangeListener(event -> {
			boolean isRomTab = event.getSelectedTab().equals(romTab);
			romLayout.setVisible(isRomTab);
			mediaLayout.setVisible(!isRomTab);
		});

		add(tabs, romLayout, mediaLayout);
	}

	/**
	 * 切换Favorite状态
	 */
	private void toggleFavorite(RomFile romFile, Button btn) {
		int newFav = romFile.getFavorite() > 0 ? 0 : 5;
		romFileRepo.getFactory().update(QRomFile.romFile)
				.set(QRomFile.romFile.favorite, newFav)
				.where(QRomFile.romFile.id.eq(romFile.getId()))
				.execute();
		romFile.setFavorite(newFav);
		btn.setText(newFav > 0 ? "★" : "☆");
	}

	/**
	 * 打开ROM编辑对话框
	 */
	private void openEditDialog(RomFile romFile) {
		RomEditForm formData = RomEditForm.fromRomFile(romFile);
		AutoForm<RomEditForm> form = VaadinForms.createAutoForm(formData, RomEditForm.class);

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("编辑ROM信息 - " + romFile.getName());
		dialog.setWidth("600px");

		Button closeButton = new Button(new Icon("lumo", "cross"), e -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getHeader().add(closeButton);

		dialog.add(form);

		Button cancelButton = new Button("取消", e -> dialog.close());
		cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getFooter().add(cancelButton);

		Button saveButton = new Button("保存", e -> {
			RomEditForm data = form.getBeanIfValid();
			if (data != null) {
				saveRomFile(romFile, data);
				dialog.close();
				romGrid.getDataProvider().refreshAll();
				Notification.show("保存成功", 2000, Notification.Position.BOTTOM_END)
						.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			}
		});
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dialog.getFooter().add(saveButton);

		dialog.open();
	}

	/**
	 * 保存ROM编辑结果
	 */
	private void saveRomFile(RomFile romFile, RomEditForm form) {
		QRomFile t = QRomFile.romFile;
		romFileRepo.getFactory().update(t)
				.set(t.name, form.getName())
				.set(t.region, form.getRegion())
				.set(t.gameType, form.getGameType())
				.set(t.favorite, form.getFavorite())
				.set(t.gameid, form.getGameid() != null ? form.getGameid() : "")
				.set(t.version, form.getVersion())
				.set(t.hackComment, form.getHackComment())
				.where(t.id.eq(romFile.getId()))
				.execute();
	}
}
