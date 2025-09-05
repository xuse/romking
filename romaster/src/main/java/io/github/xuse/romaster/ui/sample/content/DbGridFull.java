package io.github.xuse.romaster.ui.sample.content;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import io.github.xuse.SpringContextUtil;
import io.github.xuse.jetui.vaadin.component.PaginationControls;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.repo.obj.RomFileFilter;

/**
 * 用于展示数据库Grid的完整特性
 * 1、后台查询与分页
 * 2、复杂查询表单
 * 3、详情折叠展开
 * 4、操作按钮
 */
@Route("grid-db-full")
public class DbGridFull extends Div {

	private final PaginationControls paginationControls = new PaginationControls();

	//将ListDataProvider包装成Page Provider。即与分页组件绑定
	private DataProvider<RomFile, RomFileFilter> createPageProvider(){
		RomFileRepository repo=SpringContextUtil.getBean(RomFileRepository.class);
		return DataProvider.fromFilteringCallbacks(query -> {
			query.getLimit();
			query.getOffset();
			var offset = paginationControls.calculateOffset();
			var limit = paginationControls.getPageSize();
			return repo.list(query.getFilter(), offset, limit);
		}, query -> {
			var itemCount = repo.count(query.getFilter());
			paginationControls.recalculatePageCount(itemCount);
			var offset = paginationControls.calculateOffset();
			var limit = paginationControls.getPageSize();
			var remainingItemsCount = itemCount - offset;
			return Math.min(remainingItemsCount, limit);
		});
	}

	public DbGridFull() {
		Grid<RomFile> grid = new Grid<>(RomFile.class, false);
		Grid.Column<RomFile> nameColumn = grid.addColumn(createFilesRenderer()).setHeader("Name").setWidth("230px")
				.setFlexGrow(0);
		Grid.Column<RomFile> pathColumn = grid.addColumn(RomFile::getFilepath).setHeader("Path").setFlexGrow(0)
				.setSortable(true);
		Grid.Column<RomFile> idColumn = grid.addColumn(RomFile::getGameid).setHeader("IDS").setSortable(true);

		RomFileFilter filter = new RomFileFilter();

		ConfigurableFilterDataProvider<RomFile, Void, RomFileFilter> cp=createPageProvider().withConfigurableFilter();
		grid.setDataProvider(cp);
		
		//设计简单搜索框
		grid.setHeight("600px");
		TextField searchField = new TextField();
		searchField.setWidth("50%");
		searchField.setPlaceholder("Search");
		searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		searchField.setClearButtonVisible(true);
		searchField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
		searchField.setValueChangeTimeout(500);
		searchField.addValueChangeListener(e -> {
			filter.setName(searchField.getValue());
			cp.setFilter(filter);
		});

		// 可调整每页大小的分页场景下需要让表格长度全部显示
        grid.setAllRowsVisible(true); 
		paginationControls.onPageChanged(() -> grid.getDataProvider().refreshAll());
		
		//搜索框与Grid布局
		VerticalLayout layout = new VerticalLayout(searchField, grid,paginationControls);
		layout.setHeightFull();
		layout.setPadding(false);
		add(layout);
	}

	private static Renderer<RomFile> createFilesRenderer() {
		return LitRenderer
				.<RomFile>of("<vaadin-horizontal-layout style=\"align-items: center;\" theme=\"spacing\">"
						+ "  <vaadin-avatar img=\"${item.pictureUrl}\" name=\"${item.fullName}\"></vaadin-avatar>"
						+ "  <span> ${item.fullName} </span>" + "</vaadin-horizontal-layout>")
				.withProperty("pictureUrl", (o) -> o.getPlatform().iconUrl).withProperty("fullName", RomFile::getName);
	}
}