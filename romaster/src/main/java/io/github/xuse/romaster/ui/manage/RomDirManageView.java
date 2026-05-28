package io.github.xuse.romaster.ui.manage;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.obj.RomDirFilter;
import jakarta.annotation.security.PermitAll;

/**
 * 仓库目录列表视图。
 * 通过URL参数接收仓库label，展示该仓库下的所有目录。
 * 点击目录行可进入ROM文件列表。
 */
@Route("rom-dirs")
@PageTitle("ROM Directories")
@PermitAll
public class RomDirManageView extends Main implements HasUrlParameter<String> {

	private final RomConsole romconsole;
	private final RomDirRepository romDirRepo;
	private Grid<RomDir> dirGrid;
	private String repoLabel;

	public RomDirManageView(RomConsole console) {
		this.romconsole = console;
		this.romDirRepo = console.getBean(RomDirRepository.class);

		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
	}

	@Override
	public void setParameter(BeforeEvent event, String parameter) {
		this.repoLabel = parameter;
		buildUI();
	}

	private void buildUI() {
		removeAll();

		// 创建带过滤的Grid
		RomDirFilter filter = new RomDirFilter();
		if (repoLabel != null && !repoLabel.isEmpty()) {
			filter.setLabel(repoLabel);
		}

		dirGrid = new Grid<>(RomDir.class, false);
		VaadinViews.addColumnsTo(dirGrid, RomDir.class);

		// 使用ConfigurableFilterDataProvider，预设label过滤
		CallbackDataProvider<RomDir, RomDirFilter> dataProvider = new CallbackDataProvider<>(
				q -> romDirRepo.list(q.getFilter(), q.getOffset(), q.getLimit()),
				q -> romDirRepo.count(q.getFilter()));
		ConfigurableFilterDataProvider<RomDir, Void, RomDirFilter> filterProvider =
				dataProvider.withConfigurableFilter();
		filterProvider.setFilter(filter);
		dirGrid.setItems(filterProvider);

		dirGrid.setSizeFull();

		// 点击行进入ROM文件列表
		dirGrid.addItemClickListener(event -> {
			RomDir dir = event.getItem();
			UI.getCurrent().navigate("rom-files/" + dir.getId());
		});

		add(dirGrid);
	}
}
