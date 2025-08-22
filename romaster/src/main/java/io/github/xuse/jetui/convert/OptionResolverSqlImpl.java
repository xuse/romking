package io.github.xuse.jetui.convert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.github.xuse.querydsl.util.Exceptions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class OptionResolverSqlImpl extends CacheLoader<Map<String,Object>, Map<String,String>> implements OptionResolver {
	
	private final SQLQueryFactory factory;
	/**
	 * 原始的SQL语句
	 */
	private String sql;
	
	/**
	 * 解析后的参数表
	 */
	private Map<String,Integer> paramsIndex = new HashMap<>();
	
	
	/**
	 * 是否有可变参数，有可变参数意味着缓存复杂
	 */
	private boolean noParam;
	
	
	private LoadingCache<Map<String,Object>, Map<String,String>> cache;
	
	/**
	 * @param url
	 * @param factory
	 * @param cacheSeconds 缓存，传入0表示无缓存，传入正数表示缓存的秒数
	 */
	public OptionResolverSqlImpl(String url,SQLQueryFactory factory,int cacheSeconds){
		this.factory = factory;
		this.sql = url;
		this.paramsIndex = parseSQL(url);
		if(cacheSeconds>0) {
			cache=CacheBuilder.newBuilder().expireAfterWrite(cacheSeconds, TimeUnit.SECONDS).build(this);			
		}
	}

	private Map<String,Integer> parseSQL(String url) {
		//正常要用sqlparser来词法分析的。这里先简化处理
//		sql://select id as code,name as text from sdffd where lang=:user_lang
		
		
		
		
		return null;
	}

	/**
	 * 执行本地数据库查询获得键值对
	 * @param params
	 * @return Map
	 */
	public Map<String, String> getValues(Map<String, Object> params) {
		if(cache==null) {//无缓存的情况
			return load(fixParams(params));
		}else {
			try {
				return cache.get(fixParams(params));
			} catch (ExecutionException e) {
				throw new RuntimeException(e.getCause());
			}
		}
	}

	/**
	 * 用户传入的参数列表中可能有大量无关参数，此处需要过滤掉无关参数，仅保留SQL相关的几项参数进行操作。
	 * @param params
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> fixParams(Map<String, Object> params) {
		if(noParam || params==null) {
			return Collections.EMPTY_MAP;
		}
		Map<String,Object> result=new HashMap<String,Object>(params);
		
//		for(String s: query.getParameterNames()) {
//			if(params.containsKey(s)) {
//				result.put(s, params.get(s));
//			}
//		}
		return result;
	}

	@Override
	public Map<String, String> load(Map<String, Object> params){
		LinkedHashMap<String,String> map=new LinkedHashMap<>();
		try(Connection conn=factory.getConnection()){
			try(PreparedStatement st=conn.prepareStatement(sql)){
				try(ResultSet rs=st.executeQuery()){
					while(rs.next()) {
						map.put(rs.getString("code"), rs.getString("text"));
					}
				}
			}
		} catch (SQLException e1) {
			throw Exceptions.toRuntime(e1);
		}
		return map;
	}

	@Override
	public boolean isConstants() {
		return false;
	}

	@Override
	public boolean isConditional() {
		return !noParam;
	}
}
