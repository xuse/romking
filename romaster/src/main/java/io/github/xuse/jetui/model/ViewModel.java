package io.github.xuse.jetui.model;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.xuse.querydsl.util.StringUtils;

import io.github.xuse.jetui.JetUIs;
import io.github.xuse.jetui.annotation.View;
import io.github.xuse.jetui.annotation.ViewColumn;
import io.github.xuse.jetui.common.SearchOp;

/**
 * 描述动态视图
 * 
 * @author jiyi
 *
 */
public class ViewModel {
	/**
	 * 场景
	 */
	private String scenario;

	/**
	 * 对象类
	 */
	private Class<?> entityClass;

	/**
	 * 各列
	 */
	private List<ViewColumnModel> columns;

	/**
	 * 默认排序列
	 */
	private String defaultSortField;
	/**
	 * 默认倒序
	 */
	private boolean defaultDesc = false;
	/**
	 * 一对多级联是否查询
	 */
	private boolean toMany = false;
	/**
	 * 多对一是否查询
	 */
	private boolean toOne = true;

	/**
	 * 使用的组件
	 */
	private String component;

	private SearchModel searchModel;
	/**
	 * 别名倒序表
	 */
	private Map<String, String> viewToDbMapping = new HashMap<String, String>();

	public ViewModel(Class<?> clz, View view, String scenario) {
		this.entityClass = clz;
		this.scenario = scenario;
		this.columns = new ArrayList<ViewColumnModel>();
		this.searchModel=new SearchModel();
		if (view != null) {
			this.component = StringUtils.trimToNull(view.component());
			this.defaultDesc = view.defaultIsDesc();
			this.defaultSortField = StringUtils.trimToNull(view.defaultOrderField());
			this.toMany = view.cascadeToMany();
			this.toOne = view.cascadeToOne();
			if(view.defaultParam().length()>0){
				this.searchModel.setDefaultSearchParam(JetUIs.toMap(view.defaultParam(), ";", ":", 0));
			}
		}
	}

	public String getDefaultSortField() {
		return defaultSortField;
	}

	public void setDefaultSortField(String defaultSortField) {
		this.defaultSortField = defaultSortField;
	}

	public boolean isDefaultDesc() {
		return defaultDesc;
	}

	public void setDefaultDesc(boolean defaultDesc) {
		this.defaultDesc = defaultDesc;
	}

	public boolean isToMany() {
		return toMany;
	}

	public boolean isToOne() {
		return toOne;
	}

	/**
	 * 在模型中添加一个列
	 * 
	 * @param column
	 * @param jfield
	 *            如果支持点击排序，那么需要传入数据库中对应的排序列
	 */
	public void add(ViewColumnModel column, java.lang.reflect.Field field, ViewColumn annotation, AnnotatedElement element) {
		if (annotation.visible()) {
			this.columns.add(column);
			// 如果该列可点击排序
			if (column.isSortable()) {
				viewToDbMapping.put(column.getField(), getDbColumn(column, field, annotation));
			}
		}
		if (annotation.search() != SearchOp.NONE) {
			searchModel.addComplexSearch(annotation, element, getDbColumn(column, field, annotation));
		}
		if (annotation.searchSimple()) {
			searchModel.addSimpleSratch(column, getDbColumn(column, field, annotation));
		}
	}

	private String getDbColumn(ViewColumnModel column, Field field, ViewColumn annotation) {
		if (annotation.dbColumnName().length() > 0) {
			return annotation.dbColumnName();
		} else if (field != null) {
			return field.getName();
		} else {
			throw new IllegalArgumentException(column.getField() + "如果要支持排序，必须指定数据库中的排序列名。");
		}
	}

	/**
	 * 将视图中的排序列名，转换为数据库中的排序列名
	 * 
	 * @param viewColumn
	 * @return
	 */
	public String getDbOrderColumn(String viewColumn) {
		return viewToDbMapping.get(viewColumn);
	}

	public void sort() {
		Collections.sort(columns);
	}

	/**
	 * 添加特殊列：如排序，CheckBox等列
	 * 
	 * @param column
	 */
	public void addSpecialColumn(ViewColumnModel column) {
		this.columns.add(column);
	}

	public SearchModel getSearchModel() {
		return searchModel;
	}

	public boolean isSimpleSearch() {
		return !searchModel.getSimpleSearchColumns().isEmpty();
	}

	public boolean isComplexSearch() {
		return !searchModel.getFields().isEmpty();
	}

	public void setSearchModel(SearchModel searchModel) {
		this.searchModel = searchModel;
	}

	public String getScenario() {
		return scenario;
	}

	public void setScenario(String scenario) {
		this.scenario = scenario;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
	}
}
