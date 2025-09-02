package io.github.xuse.romaster.ui.manage;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.framework.vaadin.support.VaadinViews;
import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.vo.RomRepo;
import io.github.xuse.romking.service.RomMngService;
import jakarta.annotation.security.PermitAll;

@Route("rom-mng")
@PageTitle("Roms Repositories")
@Menu(order = 0, icon = "vaadin:mailbox", title = "Roms Repositories")
@PermitAll 
public class RomRepoManageView extends Main {
    final Grid<RomRepo> grid;
    
    public RomRepoManageView(RomConsole console) {

        RomMngService romServicer = console.getBean(RomMngService.class);
    	
    	RomDirRepository romDirRepo = console.getBean(RomDirRepository.class);
    	
        grid = VaadinViews.createGrid(RomRepo.class, romServicer);
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        //创建视图工具条
//        add(new ViewToolbar("Task List", ViewToolbar.group(description, dueDate, createBtn)));
        add(grid);
    }
}
