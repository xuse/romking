package io.github.xuse.romaster.ui.sample;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.romaster.service.RomService;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import jakarta.annotation.security.PermitAll;

@Route("test-list3")
@PageTitle("Test3")
@Menu(order = 6, icon = "vaadin:toolbox", title = "Test List3")
@PermitAll
public class TestReposView3 extends Main {
	
	public TestReposView3(RomService taskService, RomFileRepository repo,RomConsole console) {
		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
		TabSheet tabSheet = new TabSheet();
		tabSheet.add("Grid1", new GridItemDetailsToggle());
		tabSheet.add("Grid2", new GridManualPagination());

		
		add(tabSheet);
		setSizeFull();
	}
}