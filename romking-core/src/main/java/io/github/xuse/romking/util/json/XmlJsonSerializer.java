package io.github.xuse.romking.util.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.github.xuse.querydsl.util.StringUtils;

import io.github.xuse.romking.util.xml.XMLUtils;

public class XmlJsonSerializer implements ObjectSerializer {
	public static final String ARRAY_NODE_ATTR_NAME = "_type";
	public static final String ARRAY_NODE_ATTR_VALUE = "array";
	public static final String NAME_COMPLEX_ARRAY_ELEMENT = "e";
	public static final String NAME_PRIMITIVE_ARRAY_ELEMENT = "v";

	private static String nodeAsText(Node node) {
		short type = node.getNodeType();
		if (type == Node.ATTRIBUTE_NODE || type == Node.TEXT_NODE) {
			return StringEscapeUtils.unescapeHtml4(node.getNodeValue().trim());
		} else if (type == Node.CDATA_SECTION_NODE) {
			return ((CDATASection) node).getTextContent();
		} else {
			return null;
		}
	}

	public static void beginObject(SerializeWriter out) {
		out.append('{');
	}
	public static void endObject(SerializeWriter out) {
		out.append('}');
	}
	public static void beginArray(SerializeWriter out) {
		out.append('[');
	}
	public static void endArray(SerializeWriter out) {
		out.append(']');
	}

	public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType,int features){
		Node src=(Node)object;
		SerializeWriter writer=serializer.getWriter();
		if(src.getNodeType()==Node.ELEMENT_NODE){
			Element e=(Element)src;
			if(ARRAY_NODE_ATTR_VALUE.equals(e.getAttribute(ARRAY_NODE_ATTR_NAME))){
				beginArray(writer);
				Iterator<Element> elements=XMLUtils.childElements(src).iterator();
				if(elements.hasNext()){
					Element element=elements.next();
					writeElement(serializer,writer,element);
					while(elements.hasNext()){
						element=elements.next();
						writer.write(',');
						writeElement(serializer,writer,element);	
					}
				}
				endArray(writer);
				return ;
			}
		}else if(src.getNodeType()==Node.DOCUMENT_NODE){
			Element root=((Document)src).getDocumentElement();
			write(serializer,root,fieldName,fieldType,features);
			return ;
		}else{
			throw new IllegalArgumentException();
		}
		//DOcument
		beginObject(writer);
		boolean first=true;
		if(src.getAttributes()!=null){
			for(Node node:XMLUtils.toArray(src.getAttributes())){
				Attr attr=(Attr)node;
				String key=attr.getName();
				String value=attr.getValue();
				writeKeyValue(first,serializer,writer,key,value,features);
				first=false;
			}	
		}
		Map<String,List<Node>> childsMap=new HashMap<String,List<Node>>();
		for(Node node:XMLUtils.toArray(src.getChildNodes())){
			String name=node.getNodeName();
			List<Node> nodes=childsMap.get(name);
			if(nodes==null){
				nodes=new ArrayList<Node>();
				childsMap.put(name, nodes);
			}
			nodes.add(node);
		}
		List<Node> textNodes=childsMap.remove("#text");
		if(textNodes!=null && textNodes.size()>0){
			StringBuilder nodeText=new StringBuilder();
			for(int i=0;i<textNodes.size();i++){
				String s=nodeAsText(textNodes.get(i));
				if(StringUtils.isEmpty(s))continue;
				if(i>0)nodeText.append('\n');
				nodeText.append(s);	
			}
			if(nodeText.length()>0){
				writeKeyValue(first,serializer,writer,"#text", nodeText.toString(),features);	
				first=false;
			}
		}
		for(String key: childsMap.keySet()){
			List<Node> nodes=childsMap.get(key);
			if(nodes.size()==1){
				writeKeyValue(first,serializer,writer,key,nodes.get(0),features);
			}else{
				if(!first)
					writer.write(',');
				writer.writeFieldName(key);
				beginArray(writer);
				Iterator<Node> nodeIter=nodes.iterator();
				if(nodeIter.hasNext()){
					Node n=nodeIter.next();
					write(serializer,n,null,Node.class,features);
					while(nodeIter.hasNext()){
						writer.write(',');
						n=nodeIter.next();
						write(serializer,n,null,Node.class,features);
					}
				}
				endArray(writer);
			}
			first=false;
		}
		endObject(writer);
	}

	private void writeKeyValue(boolean first,JSONSerializer serializer, SerializeWriter writer, String key, Object value,int features) {
		if(!first){
			writer.write(',');
		}
		if("null".equals(value)){
			writer.writeFieldName(key);
			writer.writeNull();
		}else if (value instanceof Node){
			writer.writeFieldName(key);
			write(serializer,value,null,Node.class,features);
		}else{
			writer.writeFieldName(key);
			String s=String.valueOf(value);
			if("true".equals(s)||"false".equals(s)){
				writer.write(s);
			}else if(s.length()<14 &&  org.apache.commons.lang3.StringUtils.isNumeric(s)){
				writer.write(s);	
			}else{
				writer.writeString(s);	
			}
		}
	}

	private void writeElement(JSONSerializer serializer, SerializeWriter writer, Element element) {
		if(NAME_PRIMITIVE_ARRAY_ELEMENT.equals(element.getNodeName())){
			writer.writeString(element.getTextContent());
		}else{
			serializer.write(element);
		}
	}
}
