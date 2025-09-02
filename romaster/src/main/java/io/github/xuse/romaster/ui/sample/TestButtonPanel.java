package io.github.xuse.romaster.ui.sample;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;

import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.enums.WrapType;
import io.github.xuse.romking.repo.obj.QRomFile;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.util.RandomData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestButtonPanel extends Div {
	final RomConsole console;

	public TestButtonPanel(RomConsole console) {
		this.console = console;

		Button b1 = new Button("创建测试数据");
		b1.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		b1.addClickListener(this::generateTestData);
		add(b1);
		
		Button b2 = new Button("清除测试数据");
		b2.addClickListener(this::clearTestData);
		add(b2);
	}
	
	private void clearTestData(ClickEvent<Button> event) {
		RomFileRepository repo=console.getBean(RomFileRepository.class);
		repo.getFactory().getMetadataFactory().truncate(QRomFile.romFile).execute();
		log.info("TRUNCATED");
	}

	private void generateTestData(ClickEvent<Button> event) {
		
		
		
		List<RomFile> files= Arrays.asList(RandomData.newArrayInstance(RomFile.class, 15));
		
		RomFileRepository repo=console.getBean(RomFileRepository.class);
		
		QRomFile t=QRomFile.romFile;
		Object o=repo.getFactory().getConfiguration().getType(t.wrapType,WrapType.class);
		System.out.println(o.getClass());
		
		
		repo.getFactory().insert(t).populateBatch(files).execute();
		log.info("RomFile表当前有数据{}条",repo.count((e)->{}));
		
	}
}
