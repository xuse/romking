package io.github.xuse.romking.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

public final class Charsets {
	private Charsets() {
	}

	public static final Charset GB18030 = Charset.forName("GB18030");

	public static final Charset UTF8 = StandardCharsets.UTF_8;

	public static final Charset UTF_32BE = Charset.forName("UTF-32BE");

	public static final Charset UTF_32LE = Charset.forName("UTF-32LE");

	public static final Charset UTF_16BE = Charset.forName("UTF-16BE");

	public static final Charset UTF_16LE = Charset.forName("UTF-16LE");

	public static String getStdName(String name) {
		try {
			Charset cs = Charset.forName(name);
			return cs.name();
		} catch (UnsupportedCharsetException e) {
			return null;
		}
	}
}
