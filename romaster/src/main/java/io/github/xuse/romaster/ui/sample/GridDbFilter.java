package io.github.xuse.romaster.ui.sample;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.repo.obj.RomFileFilter;

@Route("grid-db-filtering")
public class GridDbFilter extends Div {

	public GridDbFilter(RomFileRepository repo) {
		Grid<RomFile> grid = new Grid<>(RomFile.class, false);
		Grid.Column<RomFile> nameColumn = grid.addColumn(createFilesRenderer()).setHeader("Name").setWidth("230px").setFlexGrow(0);
		Grid.Column<RomFile> pathColumn = grid.addColumn(RomFile::getFilepath).setHeader("Path").setFlexGrow(0).setSortable(true);
		Grid.Column<RomFile> idColumn = grid.addColumn(RomFile::getGameid).setHeader("IDS").setSortable(true);

		RomFileFilter filter = new RomFileFilter();
		CallbackDataProvider<RomFile, RomFileFilter> rawProvider = new CallbackDataProvider<RomFile, RomFileFilter>(
				(q) -> repo.list(q.getFilter(), q.getOffset(), q.getLimit()), (q) -> {
					return repo.count(q.getFilter());
				});
		ConfigurableFilterDataProvider<RomFile, Void, RomFileFilter> cp=rawProvider.withConfigurableFilter();
		GridDataView<RomFile> dataView = grid.setItems(cp);		

		TextField searchField = new TextField();
		searchField.setWidth("50%");
		searchField.setPlaceholder("Search");
		searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		searchField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
		searchField.setValueChangeTimeout(500);
		searchField.addValueChangeListener(e -> {
			filter.setName(searchField.getValue());
			cp.setFilter(filter);
		});

		VerticalLayout layout = new VerticalLayout(searchField, grid);
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