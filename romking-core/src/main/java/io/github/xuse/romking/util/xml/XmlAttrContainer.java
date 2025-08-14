package io.github.xuse.romking.util.xml;

import java.util.Map;

public interface XmlAttrContainer {
	Map<String,String> getAttributes();
	
	void attribute(String key, String value);
}
