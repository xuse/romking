package io.github.xuse.simple.context.util.xml;

import java.util.Map;

public interface XmlAttrContainer {
	Map<String,String> getAttributes();
	
	void attribute(String key, String value);
}