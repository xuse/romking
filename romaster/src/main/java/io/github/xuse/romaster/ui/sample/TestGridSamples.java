package io.github.xuse.romaster.ui.sample;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.romaster.service.RomService;
import io.github.xuse.romaster.ui.sample.content.GridColumnFiltering;
import io.github.xuse.romaster.ui.sample.content.GridDbFilteing;
import io.github.xuse.romaster.ui.sample.content.GridItemDetailsToggle;
import io.github.xuse.romaster.ui.sample.content.GridManualPagination;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import jakarta.annotation.security.PermitAll;

@Route("test-list3")
@PageTitle("测试样例")
@Menu(order = 6, icon = "vaadin:piggy-bank", title = "测试样例")
@PermitAll
public class TestGridSamples extends Main {
	
	public TestGridSamples(RomService taskService, RomFileRepository repo,RomConsole console) {
		addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
				LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
		TabSheet tabSheet = new TabSheet();
		tabSheet.add("手动内存翻页", new GridManualPagination());
		tabSheet.add("内存即时过滤", new GridColumnFiltering());
		tabSheet.add("后台翻页和检索", new GridDbFilteing(repo));
		tabSheet.add("详情展开表格", new GridItemDetailsToggle());

		
		add(tabSheet);
		setSizeFull();
	}
}