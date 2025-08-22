package io.github.xuse.jetui.common;

import java.util.List;

import com.alibaba.fastjson.annotation.JSONType;

/**
 * 用于传送Page对象到前台的VO
 * 
 * @author jiyi
 *
 * @param <T>
 */
@JSONType(ignores = { "clz", "scenario" })
public class Page<T> {
	/**
	 * 视图模型的类
	 */
	private transient Class<T> clz;
	/**
	 * 视图的场景
	 */
	private transient String scenario;
	
	private List<T> data;
	private int total;

	public Page() {
	}

	public Page(List<T> data, int total, Class<T> clz, String scenario) {
		this.total = total;
		this.data = data;
		this.clz = clz;
		this.scenario = scenario;
	}

	public boolean isSuccess() {
		return true;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public Class<T> getClz() {
		return clz;
	}

	public String getScenario() {
		return scenario;
	}
}
