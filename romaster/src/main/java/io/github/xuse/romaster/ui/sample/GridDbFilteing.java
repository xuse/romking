package io.github.xuse.romaster.ui.sample;

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

import io.github.xuse.romaster.ui.sample.GridManualPagination.PaginationControls;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.repo.obj.RomFileFilter;

@Route("grid-db-filtering")
public class GridDbFilteing extends Div {

	private final PaginationControls paginationControls = new PaginationControls();

	
	private DataProvider<RomFile, RomFileFilter> createPageProvider(RomFileRepository repo){
		return DataProvider.fromFilteringCallbacks(query -> {
			query.getLimit();
			query.getOffset();
			var offset = paginationControls.calculateOffset();
			var limit = paginationControls.getPageSize();
			return repo.list(query.getFilter(), offset, limit);
		}, query -> {
			var itemCount = repo.count(query.getFilter());

			// Recalculate page count here to avoid calling
			// dataSource.count twice
			paginationControls.recalculatePageCount(itemCount);

			var offset = paginationControls.calculateOffset();
			var limit = paginationControls.getPageSize();

			// Return the number of items for the current page, taking the remaining items
			// on the last page into consideration
			var remainingItemsCount = itemCount - offset;
			return Math.min(remainingItemsCount, limit);
		});
	}

	public GridDbFilteing(RomFileRepository repo) {
		Grid<RomFile> grid = new Grid<>(RomFile.class, false);
		Grid.Column<RomFile> nameColumn = grid.addColumn(createFilesRenderer()).setHeader("Name").setWidth("230px")
				.setFlexGrow(0);
		Grid.Column<RomFile> pathColumn = grid.addColumn(RomFile::getFilepath).setHeader("Path").setFlexGrow(0)
				.setSortable(true);
		Grid.Column<RomFile> idColumn = grid.addColumn(RomFile::getGameid).setHeader("IDS").setSortable(true);

		RomFileFilter filter = new RomFileFilter();
//		CallbackDataProvider<RomFile, RomFileFilter> rawProvider = new CallbackDataProvider<RomFile, RomFileFilter>(
//				(q) -> repo.list(q.getFilter(), q.getOffset(), q.getLimit()), (q) -> {
//					return repo.count(q.getFilter());
//				});
		ConfigurableFilterDataProvider<RomFile, Void, RomFileFilter> cp=createPageProvider(repo).withConfigurableFilter();
		grid.setDataProvider(cp);
		
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

        grid.setAllRowsVisible(true); // this will prevent scrolling in the grid
        
		paginationControls.onPageChanged(() -> grid.getDataProvider().refreshAll());
		
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