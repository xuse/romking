package io.github.xuse.jetui.convert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.github.xuse.querydsl.util.StringUtils;
import com.mysema.commons.lang.Pair;

import io.github.xuse.jetui.convert.DecoratorResolver.Decorator;

/**
 * 界面的枚举选项解析器实现。
 * 
 * @author jiyi
 *
 */
@Service
public class SelectMapResolverImpl implements SelectMapResolver {
	@Autowired
	private SQLQueryFactory factory;

	@Override
	public OptionResolver parse(String url) {
		if (StringUtils.isEmpty(url)) {
			return new OptionResolverEnum(Collections.<String, String>emptyMap());
		}
		OptionResolver o = null;
		Decorator dec = null;
		if (url.startsWith("enum:")) {
			Pair<String,Decorator> keys = getDecorator(url.substring(5));
			dec = keys.getSecond();
			o = new OptionResolverEnum(keys.getFirst());
		} else if (url.startsWith("properties:")) {
			Pair<String,Decorator> keys = getDecorator(url.substring(11));
			dec = keys.getSecond();
			o = new OptionResolverPropsImpl(keys.getFirst());
		} else if (url.startsWith("sql:")) {// 缓存时间
			Pair<String,Decorator> keys = getDecorator(url.substring(4));
			dec = keys.getSecond();
			o = new OptionResolverSqlImpl(keys.getFirst(), factory, dec == null ? 20 : dec.getCacheSeconds());
		} else if (url.startsWith("dict:")) {
			Pair<String,Decorator> keys = getDecorator(url.substring(5));
			dec = keys.getSecond();
			String sql = "select code,text from data_dict where type='" + keys.getFirst() + "'";
			dec = keys.getSecond();
			o = new OptionResolverSqlImpl(sql, factory, dec == null ? 60 : dec.getCacheSeconds());
		} else if (url.startsWith("call:")) {
			Pair<String,Decorator> keys = getDecorator(url.substring(5));
			o = new OptionResolverCall(keys.getFirst());
		}
		if (o == null) {
			throw new UnsupportedOperationException(url);
		}
		if (dec != null) {
			o = new DecoratorResolver(dec, o);
		}
		return o;
	}

	private Pair<String,Decorator> getDecorator(String url) {
		int index = url.indexOf("//");

		String dec = url.substring(0, index);
		String sql = url.substring(index + 2);
		if (dec.length() == 0) {
			return Pair.of(sql, null);
		}
		Decorator d = new Decorator();
		JSONObject json = JSON.parseObject(dec);
		if (json.containsKey("cache")) {
			d.setCacheSeconds(StringUtils.toInt(json.getString("cache"), 20));
		}
		if (json.containsKey("add")) {
			JSONObject entries = json.getJSONObject("add");
			d.setAddAfter(convert(entries));

		}
		if (json.containsKey("addBefore")) {
			JSONObject entries = json.getJSONObject("addBefore");
			d.setAddBefore(convert(entries));
		}
		d.setWithCode(json.getBooleanValue("withCode"));
		d.setResort(json.getBooleanValue("resort"));
		d.setForceResort(json.getBooleanValue("forceResort"));
		return Pair.of(sql, d);
	}

	private List<Pair<String,String>> convert(JSONObject entries) {
		List<Pair<String,String>> list = new ArrayList<Pair<String,String>>(entries.size());
		for (Map.Entry<String, Object> entry : entries.entrySet()) {
			list.add(Pair.of(entry.getKey(), String.valueOf(entry.getValue())));
		}
		return list;
	}

}
