package io.github.xuse.jetui.model;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONType;

import io.github.xuse.jetui.common.SearchOp;

/**
 * 在视图中产生一个列。
 * @author jiyi
 *
 */
@JSONType(ignores="order")
public class ViewColumnModel implements Comparable<ViewColumnModel>{
	public static final ViewColumnModel SERIAL=new ViewColumnModel("_serial",null).setOrder(-2);; 
	public static final ViewColumnModel CHECK=new ViewColumnModel("_check",null).setOrder(-1);
	
	private String field;
	private String text;
	private Integer weight;
	private String align;
	private boolean sortable;
	private boolean searchSimple;
	private SearchOp search;
	private int order;
	private Map<String,String> attribute;
	

	public ViewColumnModel(String field, String text) {
		this.field = field;
		this.text = text;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public String getAlign() {
		return align;
	}

	public void setAlign(String align) {
		this.align = align;
	}

	public int getOrder() {
		return order;
	}

	public ViewColumnModel setOrder(int order) {
		this.order = order;
		return this;
	}

	public boolean isSortable() {
		return sortable;
	}

	public void setSortable(boolean sortable) {
		this.sortable = sortable;
	}

	public SearchOp getSearch() {
		return search;
	}

	public void setSearch(SearchOp search) {
		this.search = search;
	}
	
	public boolean isSearchSimple() {
		return searchSimple;
	}

	public void setSearchSimple(boolean searchSimple) {
		this.searchSimple = searchSimple;
	}

	public void addAttribute(String key,String value) {
		if (attribute == null) {
			attribute = new HashMap<>();
		}
		attribute.put(key, value);
	}
	
	public Map<String, String> getAttribute() {
		return attribute;
	}

	public void setAttribute(Map<String, String> attribute) {
		this.attribute = attribute;
	}

	@Override
	public int compareTo(ViewColumnModel o) {
		return Integer.compare(this.order, o.order);
	}
	
}
