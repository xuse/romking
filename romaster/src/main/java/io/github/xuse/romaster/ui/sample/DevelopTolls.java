package io.github.xuse.romaster.ui.sample;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.romaster.service.RomService;
import io.github.xuse.romaster.ui.sample.content.DbGridFull;
import io.github.xuse.romaster.ui.sample.content.ShowIcons;
import io.github.xuse.romaster.ui.sample.content.TestButtonPanel;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import jakarta.annotation.security.PermitAll;

@Route("test-list2")
@PageTitle("开发工具")
@Menu(order = 6, icon = "vaadin:automation", title = "开发工具")
@PermitAll
public class DevelopTolls extends Main {
	public DevelopTolls(RomService taskService, RomFileRepository repo,RomConsole console) {
		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
		TabSheet tabSheet = new TabSheet();
		
		tabSheet.add("图标一览", new ShowIcons());
		tabSheet.add("开发者操作", new TestButtonPanel(console));
		tabSheet.add("Grid完整", new DbGridFull());
		
		add(tabSheet);
		setSizeFull();
	}
}