package io.github.xuse.romking.manager.obj;

import java.io.File;

import org.w3c.dom.Element;

import com.github.xuse.querydsl.util.StringUtils;

import io.github.xuse.romking.util.xml.XMLUtils;
import lombok.Data;

@Data
public class Game {
	private String path;
	private String name;
	private String image;
	private String video;
	
	/**
	 * 所在目录
	 */
	private File baseDir;
	
	/**
	 * 文件校验码
	 */
	private String md5;
	/**
	 * 文件大小
	 */
	private long length;
	
	
	private RomState romState = RomState.INIT;
	
	/**
	 * 图片/视频文件总数
	 */
	private int imageVideoTotal;
	/**
	 * 图片/视频文件就绪数
	 */
	private int imageVideoAvailble;
	
	public static Game parseFrom(File gamelist, Element element) {
		String name=XMLUtils.nodeText(element, "name");
		String image=XMLUtils.nodeText(element, "image");
		String path=XMLUtils.nodeText(element, "path");
		String video=XMLUtils.nodeText(element, "video");
		Game game=new Game();
		game.name=name;
		game.path=StringUtils.trimToNull(path);
		game.video=StringUtils.trimToNull(video);
		game.image=StringUtils.trimToNull(image);
		game.baseDir=gamelist.getParentFile();
		game.checkState();
		return game;
	}

	private void checkState() {
		if(StringUtils.isBlank(this.path)) {
			romState = RomState.MISS_FILE;
		}else {
			File rom=new File(this.baseDir,this.path);
			if(rom.isFile()) {
				romState = RomState.FILE_EXIST;
				this.length=rom.length();
				//this.md5
			}else {
				System.err.println("["+name+"]的ROM文件丢失："+rom.getAbsolutePath());
			}
		}
		if(!StringUtils.isBlank(image)) {
			imageVideoTotal++;
			File cover=new File(this.baseDir,this.image);
			if(cover.isFile() && cover.length()>0) {
				imageVideoAvailble++;
			}else {
				System.err.println("["+name+"]的图片文件丢失："+cover.getAbsolutePath());
			}
		}
		if(!StringUtils.isBlank(video)) {
			imageVideoTotal++;
			File cover=new File(this.baseDir,this.video);
			if(cover.isFile() && cover.length()>0) {
				imageVideoAvailble++;
			}else {
				System.err.println("["+name+"]的视频文件丢失："+cover.getAbsolutePath());
			}
		}		
	}
	public String toString() {
		return name+"@"+path;
	}
	
	
}

