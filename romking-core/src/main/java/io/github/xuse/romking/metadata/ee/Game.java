package io.github.xuse.romking.metadata.ee;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.xuse.romking.util.xml.XMLField;
import io.github.xuse.romking.util.xml.XmlAttrContainer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Game implements XmlAttrContainer{
	private String path;
	private String name;
	private String desc;
	private String image;
	
	private String thumbnail;
	private String video;
	
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
	
	
	
}
