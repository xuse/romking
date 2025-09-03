package io.github.xuse.romking.metadata.ee;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.xuse.querydsl.util.StringUtils;

import io.github.xuse.simple.context.util.xml.XMLField;
import io.github.xuse.simple.context.util.xml.XmlAttrContainer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Game implements XmlAttrContainer{
	
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
	
	
	private String path;
	private String name;
	private String image;
	private String video;
	
	private String desc;
	private String thumbnail;
	
	
	private Float rating;
	private String releasedate;
	private String developer;
	
	private String publisher;
	private String genre;
	private Integer players;
	private Integer playcount;
	private String lastplayed;
	private String marquee;
	private Map<String,String> attributes;
	
	@XMLField("sort-by")
	private Integer sortBy;
	
	@Override
	public void attribute(String key, String value) {
		if(attributes == null) {
			attributes=new LinkedHashMap<>();
		}
		attributes.put(key, value);
	}
	
	public void checkState() {
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
