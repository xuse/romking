package io.github.xuse.jetui.model;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.xuse.jetui.annotation.ViewColumn;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.jetui.common.SearchOp;

/**
 * 描述一个视图中支持的搜索行为
 * @author jiyi
 *
 */
public class SearchModel {
	/**
	 * 复杂搜索的各个框，如果为null或空说明复杂搜索框无需出现
	 */
	private final List<FormFieldModel> searchInputs= new ArrayList<>();
	
	private final Map<String,SearchOp> searchOps=new HashMap<String,SearchOp>();
	
	private Map<String,String> defaultSearchParam;
	
	
	public void setDefaultSearchParam(Map<String, String> defaultSearchParam) {
		this.defaultSearchParam = defaultSearchParam;
	}

	/**
	 * 决定简单搜索框是否需要出现，此处存储哪些参与简单搜索的数据库列名
	 * 如果为null或空说明简单搜索框无需出现
	 */
	private final List<String> simpleSearchColumns = new ArrayList<String>();

	/**
	 * 
	 * @return 获得各个输入框
	 */
	public List<FormFieldModel> getFields() {
		return searchInputs;
	}
	
	/**
	 * 
	 * @return 获得各个参与简单搜索的字符
	 */
	public List<String> getSimpleSearchColumns() {
		return simpleSearchColumns;
	}


	/**
	 * 添加一个简单搜索框
	 * @param column
	 */
	public void addSimpleSratch(ViewColumnModel column,String dbColumn) {
		simpleSearchColumns.add(dbColumn);
	}

	/**
	 * 添加一个复杂搜索框
	 * @param element
	 * @param column
	 * @param dbColumn
	 */
	public void addComplexSearch(ViewColumn column,AnnotatedElement element,String dbColumn) {
		//BETWEEN需要对象两个Input框
		searchOps.put(dbColumn, column.search());
		if(column.search()==SearchOp.BETWEEN) {
//			FormFieldModel model1=FormModelCacheLoader.createFieldModel(dbColumn, element);
//			model1.setBetweenInput(true);
//			searchInputs.add(check(model1));
		}else {
			//其他操作仅需对应一个搜索输入框
//			FormFieldModel model=FormModelCacheLoader.createFieldModel(dbColumn, element);
//			model.setRequired(false);
//			searchInputs.add(check(model));
		}
		
	}
	
	public Map<String, String> getDefaultSearchParam() {
		return defaultSearchParam;
	}

	/**
	 * 取的搜索操作符
	 * @param column
	 * @return
	 */
	public SearchOp getSearchOp(String column) {
		return searchOps.get(column);
		
	}

	private FormFieldModel check(FormFieldModel model) {
		model.setRequired(false);
		if(model.getType()==InputType.TEXTAREA) {
			model.setType(InputType.TEXT);
		}else if(model.getType()==InputType.HIDDEN) {
			model.setType(InputType.TEXT);
		}
		model.setStyle(null);
		model.setCls("");
		model.setAttribute(null);
		//model.getAttribute().clear();//清除掉value属性。
		return model;
	}
}
