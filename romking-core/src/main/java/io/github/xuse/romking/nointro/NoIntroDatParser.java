package io.github.xuse.romking.nointro;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.github.xuse.simple.context.util.xml.XMLUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * No-Intro标准DAT文件解析器。
 * 解析CLRMAMEPro XML格式的DAT文件，提取游戏ROM的hash信息。
 * 
 * DAT文件格式示例：
 * <pre>
 * &lt;datafile&gt;
 *   &lt;header&gt;
 *     &lt;name&gt;Nintendo - Game Boy Advance&lt;/name&gt;
 *     &lt;version&gt;20250520-091234&lt;/version&gt;
 *   &lt;/header&gt;
 *   &lt;game name="Super Mario (Japan)"&gt;
 *     &lt;description&gt;Super Mario (Japan)&lt;/description&gt;
 *     &lt;rom name="Super Mario (Japan).gba" size="8388608" 
 *          crc="9D4F1E18" md5="B63B2244..." sha1="FC6163F9..."/&gt;
 *   &lt;/game&gt;
 * &lt;/datafile&gt;
 * </pre>
 */
@Slf4j
public class NoIntroDatParser {

	/**
	 * 解析标准DAT文件
	 */
	public DatFile parse(File file) {
		Document doc = XMLUtils.loadDocument(file);
		Element root = doc.getDocumentElement();

		DatFile datFile = new DatFile();

		// 解析header
		Element header = XMLUtils.first(root, "header");
		if (header != null) {
			datFile.setName(XMLUtils.nodeText(header, "name"));
			datFile.setVersion(XMLUtils.nodeText(header, "version"));
			datFile.setDescription(XMLUtils.nodeText(header, "description"));
		}

		// 解析game条目
		List<DatGame> games = new ArrayList<>();
		for (Element gameEl : XMLUtils.childElements(root, "game")) {
			DatGame game = parseGame(gameEl);
			if (game != null) {
				games.add(game);
			}
		}
		// 有些DAT用 "machine" 而非 "game"
		for (Element gameEl : XMLUtils.childElements(root, "machine")) {
			DatGame game = parseGame(gameEl);
			if (game != null) {
				games.add(game);
			}
		}
		datFile.setGames(games);

		log.info("解析DAT完成: {} - {} 条目, 版本 {}", datFile.getName(), games.size(), datFile.getVersion());
		return datFile;
	}

	private DatGame parseGame(Element gameEl) {
		DatGame game = new DatGame();
		game.setName(gameEl.getAttribute("name"));

		// cloneof属性（P/C DAT中存在）
		String cloneOf = gameEl.getAttribute("cloneof");
		if (cloneOf != null && !cloneOf.isEmpty()) {
			game.setCloneOf(cloneOf);
		}

		// description
		Element descEl = XMLUtils.first(gameEl, "description");
		if (descEl != null) {
			game.setDescription(XMLUtils.nodeText(descEl));
		}

		// 解析rom条目（一个game可能有多个rom，如多文件游戏）
		List<DatRom> roms = new ArrayList<>();
		for (Element romEl : XMLUtils.childElements(gameEl, "rom")) {
			DatRom rom = new DatRom();
			rom.setName(romEl.getAttribute("name"));
			rom.setSize(parseLong(romEl.getAttribute("size")));
			rom.setCrc(romEl.getAttribute("crc"));
			rom.setMd5(romEl.getAttribute("md5"));
			rom.setSha1(romEl.getAttribute("sha1"));
			// status属性（verified/good/baddump等）
			rom.setStatus(romEl.getAttribute("status"));
			roms.add(rom);
		}
		game.setRoms(roms);

		return game;
	}

	private long parseLong(String s) {
		if (s == null || s.isEmpty()) return 0;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// ========== 数据结构 ==========

	@Data
	public static class DatFile {
		private String name;
		private String version;
		private String description;
		private List<DatGame> games;
	}

	@Data
	public static class DatGame {
		private String name;
		private String description;
		/** P/C DAT中的cloneof属性，指向parent game的name */
		private String cloneOf;
		private List<DatRom> roms;
	}

	@Data
	public static class DatRom {
		private String name;
		private long size;
		private String crc;
		private String md5;
		private String sha1;
		private String status;
	}
}
