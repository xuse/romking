package io.github.xuse.romking.metadata.ee;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.github.xuse.querydsl.util.StringUtils;

import io.github.xuse.romking.metadata.Metadata;
import io.github.xuse.romking.metadata.MetadataParser;
import io.github.xuse.simple.context.util.xml.XMLUtils;

public class GamelistParser implements MetadataParser {
	@Override
	public Metadata parse(File file) {
		Document doc = XMLUtils.loadDocument(file);
		
		return null;
	}

	public static Game parseFrom(File gamelist, Element element) {
		String name = XMLUtils.nodeText(element, "name");
		String image = XMLUtils.nodeText(element, "image");
		String path = XMLUtils.nodeText(element, "path");
		String video = XMLUtils.nodeText(element, "video");
		Game game = new Game();
		game.setName(name);
		game.setPath(StringUtils.trimToNull(path));
		game.setVideo(StringUtils.trimToNull(video));
		game.setImage(StringUtils.trimToNull(image));
		game.setBaseDir(gamelist.getParentFile());
		game.checkState();
		return game;
	}

}
