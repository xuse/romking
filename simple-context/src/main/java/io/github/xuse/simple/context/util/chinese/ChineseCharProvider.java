package io.github.xuse.simple.context.util.chinese;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;

import com.github.xuse.querydsl.util.Assert;
import com.github.xuse.querydsl.util.IOUtils;

/**
 * 提供中文字符集
 * @author jiyi
 *
 */
public class ChineseCharProvider {
	static ChineseCharProvider instance;

	public enum Type {
		/**
		 * 百家姓
		 */
		CHINESE_LAST_NAME
	}

	public static ChineseCharProvider getInstance() {
		if (instance == null)
			instance = new ChineseCharProvider();

		return instance;
	}

	public char[] get(ChineseCharProvider.Type type) {
		Assert.notNull(type);
		checkAndInit();
		switch (type) {
		case CHINESE_LAST_NAME:
			return LAST_NAMES.get();
		default:
			break;
		}
		throw new UnsupportedOperationException();
	}

	private SoftReference<char[]> LAST_NAMES;

	private void checkAndInit() {
		if (LAST_NAMES == null || LAST_NAMES.get() == null) {
			LAST_NAMES = new SoftReference<char[]>(load("lastname.properties", 200));
		}
	}

	private static char[] load(String file, int i) {
		try {
			URL url=ChineseCharProvider.class.getResource(file);
			BufferedReader in = IOUtils.getUTF8Reader(url);
			CharArrayWriter sb = new CharArrayWriter(i);
			String s;
			while ((s = in.readLine()) != null) {
				sb.write(s);
			}
			return sb.toCharArray();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

}
