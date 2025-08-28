package io.github.xuse.jetui;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.xuse.querydsl.util.StringUtils;

public class JetUIs {
	/**
	 * 
	 * 
	 * @param source
	 *            源数据
	 * @param entrySep
	 *            entry间的分隔符
	 * @param keyValueSep
	 *            key/value的分隔符
	 * @param keyUpper
	 *            key是否转大写，0不修改， -1转小写， 1转大写
	 * 
	 * @return 将一串文本解析为Key/Value的若干值 对
	 */
	public static Map<String, String> toMap(String source, String entrySep, String keyValueSep, int keyUpper) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		if (source != null) {
			for (String entry : StringUtils.split(source, entrySep)) {
				entry = entry.trim();
				int index = entry.indexOf(keyValueSep);
				if (index > -1) {
					String key = entry.substring(0, index).trim();
					if (keyUpper > 0) {
						key = key.toUpperCase();
					} else if (keyUpper < 0) {
						key = key.toLowerCase();
					}
					result.put(key, entry.substring(index + 1).trim());
				} else {
					String key = entry;
					if (keyUpper > 0) {
						key = key.toUpperCase();
					} else if (keyUpper < 0) {
						key = key.toLowerCase();
					}
					result.put(key, "");
				}
			}
		}
		return result;
	}

	/**
	 * 
	 * 
	 * @param map
	 * @param entrySep
	 * @param keyValueSep
	 * @return {@link #toMap}的逆运算，将map转回到string 
	 */
	public static String toString(Map<String, String> map, String entrySep, String keyValueSep) {
		StringBuilder sb = new StringBuilder();
		Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator();
		if (iter.hasNext()) {
			{
				Map.Entry<String, String> e = iter.next();
				sb.append(e.getKey()).append(keyValueSep).append(e.getValue());
			}
			for (; iter.hasNext();) {
				Map.Entry<String, String> e = iter.next();
				sb.append(entrySep).append(e.getKey()).append(keyValueSep).append(e.getValue());
			}
		}
		return sb.toString();
	}
	
}
