package io.github.xuse.romking.metadata.ee;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.github.xuse.romking.util.xml.XMLUtils;
import io.github.xuse.simple.context.Service;
import lombok.SneakyThrows;

@Service
public class GameListService {
	
	@SneakyThrows
	public Gamelist loadXml(File file) {
		Document doc=XMLUtils.loadDocument(file);
		List<Game> games=new ArrayList<>();
		for(Element gNode:XMLUtils.childElements(doc.getDocumentElement(), "game")) {
			games.add(parse(gNode));
			
		};
		return new Gamelist(games);
	}
	
	public void saveXml(Gamelist gamelist, File file) {
		Document doc=XMLUtils.newDocument();
		Element root=XMLUtils.addElement(doc, "games");
		for(Game game:gamelist.getGames()) {
			XMLUtils.putBean(root, game);
		}
		XMLUtils.saveDocument(doc, file);
	}
	
	private Game parse(Element e) {
		return XMLUtils.loadBean(e, Game.class);
	}

}
