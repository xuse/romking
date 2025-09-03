package io.github.xuse.romking.manager.obj;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.github.xuse.querydsl.util.IOUtils;
import com.github.xuse.querydsl.util.StringUtils;

import io.github.xuse.romking.manager.RomManager;
import io.github.xuse.romking.util.chinese.PinyinUtil;
import io.github.xuse.romking.util.xml.XMLUtils;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public class RomDir {
	private final String name;
	private final File gamelist;
	private final List<Game> games =new ArrayList<>();
	private int good = 0;
	
	
	private final Map<String,Game> romIndex=new HashMap<>();
	private final Map<String,Game> mediaIndex=new HashMap<>();
	
	public void scanNewRom() {
		scanDir(gamelist.getParentFile(), 
				TaskContext.of(this::generateNewRomXml)
				.matchExt("chd","pbp","iso","bin","img")
				.setParam("imgDir", new File(gamelist.getParent(),"images"))
				.setParam("forceImage", true)
		);
	}
	
	
	public RomDir(File g, String name) {
		this.gamelist=g;
		this.name=name;
		parse();
	}

	@SneakyThrows
	public boolean parse() {
		Document doc=XMLUtils.loadDocument(gamelist);
		for(Element element:XMLUtils.childElements(doc.getDocumentElement(), "game")) {
			Game game=Game.parseFrom(gamelist,element);
			games.add(game);
			if(game.getRomState()==RomState.FILE_EXIST) {
				good++;
			}
			if(game.getPath()!=null) {
				romIndex.put(normalize(game.getPath()), game);
			}
			if(game.getImage()!=null) {
				mediaIndex.put(normalize(game.getImage()), game);
			}
			if(game.getVideo()!=null) {
				mediaIndex.put(normalize(game.getVideo()), game);
			}
		}
		return true;
	}
	
	private static String normalize(String path) {
		path=path.replace('\\', '/');
		if(path.startsWith("../")) {
			path=path.substring(3);
		}
		if(path.startsWith("./")) {
			path=path.substring(2);
		}			
		return path;
	}


	

	private void generateNewRomXml(File file, TaskContext context) {
		
		String base=gamelist.getParentFile().getAbsolutePath(); 
		String path=IOUtils.getRelativepath(file.getAbsolutePath(),base);
		
		path=path.replace('\\', '/');
		Game indexed=romIndex.get(path);
		if(indexed!=null) {
			return;
		}
		
		
	
		String image ="";
		{
			File imgDir=context.getParam("imgDir",File.class);
			if(imgDir!=null &&imgDir.exists()) {
				File testImage=RomManager.findImage(imgDir, IOUtils.removeExt(file.getName()));
				if(testImage==null) {
					System.out.println(file.getAbsolutePath()+"没有找到图片");
				}else {
					image=IOUtils.getRelativepath(testImage.getAbsolutePath(), base);
					image=image.replace('\\', '/');
				}
			}
		}
		String gameName=IOUtils.removeExt(file.getName());
		if(context.isParamEq("forceImage", true)) {
			if(StringUtils.isEmpty(image)) {
				return;
			}
		}
		
		Document doc=XMLUtils.newDocument("game");
		XMLUtils.addElement(doc.getDocumentElement(),"gameid", "");
		XMLUtils.addElement(doc.getDocumentElement(),"path", "./"+path);
		XMLUtils.addElement(doc.getDocumentElement(),"image","./"+image );
		XMLUtils.addElement(doc.getDocumentElement(),"video_id", "x");
		XMLUtils.addElement(doc.getDocumentElement(),"class_type", "9");
		XMLUtils.addElement(doc.getDocumentElement(),"game_type", "0");
		XMLUtils.addElement(doc.getDocumentElement(),"timer", "psx");
		
		XMLUtils.addElement(doc.getDocumentElement(),"zh_CN", gameName);
		XMLUtils.addElement(doc.getDocumentElement(),"en_US", gameName);
		XMLUtils.addElement(doc.getDocumentElement(),"name", PinyinUtil.getPinYinHeadChar(gameName));
		XMLUtils.output(doc.getDocumentElement(), System.out);
	}
	
	public void scanUnusedFile() {
		scanDir(gamelist.getParentFile(), TaskContext.of(this::promptUnUsed));
	}
	
	
	static class TaskContext{
		private Set<String> matchExts = new HashSet<>();
		
		private Set<String> ignoreExts = new HashSet<>(Arrays.asList("xml","txt","md","fs","ini"));
		
		private Map<String,Object> taskParams=new HashMap<>();
		
		private BiConsumer<File,TaskContext> process;
		
		public boolean isIgnore(String ext) {
			if(ignoreExts.contains(ext)) {
				return true;
			}
			if(!matchExts.isEmpty()) {
				return !matchExts.contains(ext);
			}
			return false;
		}
		
		public boolean isParamEq(String string, Object b) {
			Object o=taskParams.get(string);
			return Objects.equals(o, b);
		}

		@SuppressWarnings("unchecked")
		public <T> T getParam(String string, Class<T> clz) {
			Object o=taskParams.get(string);
			if(o==null) {
				return null;
			}
			if(clz.isInstance(o)) {
				return (T)o;
			}
			return null;
		}

		public TaskContext setParam(String key,Object value) {
			taskParams.put(key, value);
			return this;
		}

		public TaskContext clearIngore() {
			ignoreExts.clear();
			return this;
		}
		
		public TaskContext addIngore(String... exts) {
			ignoreExts.addAll(Arrays.asList(exts));
			return this;
		}

		public TaskContext matchExt(String... exts) {
			matchExts.clear();
			 matchExts.addAll(Arrays.asList(exts));
			 return this;
		}

		public static TaskContext of(BiConsumer<File, TaskContext> object) {
			TaskContext c=new TaskContext();
			c.process=object;
			return c;
		}
	}
	
	private void promptUnUsed(File file, TaskContext context) {
		Game game = null;
		String base=gamelist.getParentFile().getAbsolutePath(); 
		String path=IOUtils.getRelativepath(file.getAbsolutePath(),base);
		path=path.replace('\\', '/');
		String ext=IOUtils.getExtName(file.getName());
		if(isMedia(ext)) {
			game=mediaIndex.get(path);
		}else {
			game=romIndex.get(path);
		}
		if(game==null) {
			if(file.length()>512) {
				System.err.println("文件不属于任何游戏"+file.getAbsolutePath());
			}
		}
	}

	private void scanDir(File dir, TaskContext context) {
		
		for(File file:dir.listFiles()) {
			if(file.isDirectory()) {
				scanDir(file,context);
			}else {
				String ext=IOUtils.getExtName(file.getName());
				if(context.isIgnore(ext)) {
					continue;
				}
				if(context.process!=null) {
					context.process.accept(file, context);
				}
			}
		}
	}

	private boolean isMedia(String ext) {
		return ext.equals("jpg") ||ext.equals("png")||ext.equals("mp4")||ext.equals("flv")||ext.equals("mp3");
	}

	public int size() {
		return games.size();
	}

	public boolean removeInvalid() {
		boolean removed=false;
		for(Iterator<Game> iter=games.iterator();iter.hasNext();) {
			Game game=iter.next();
			if(game.getRomState()==RomState.MISS_FILE) {
				iter.remove();
				System.out.println("应删除游戏"+game.getName());
				removed=true;
				this.romIndex.remove(normalize(game.getPath()));
				if(game.getImage()!=null) {
					this.mediaIndex.remove(normalize(game.getImage()));
				}
				if(game.getVideo()!=null) {
					this.mediaIndex.remove(normalize(game.getVideo()));
				}
			}
		}
		return removed;
	}

	public void writeXML() {
//		File tmp=new File(gamelist.getParent(),gamelist.getName()+".tmp");
//		Document doc=XMLUtils.newDocument();
//		Element root=XMLUtils.addElement(doc, "gameList");
//		for(Game game:games) {
//			Element g=XMLUtils.addElement(root, "game");
//		}
//		BufferedWriter writer=IOUtils.getWriter(tmp, IO)
		
	}
}
