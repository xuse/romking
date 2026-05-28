package io.github.xuse.jetui.vaadin.support;

/**
 * 通用格式化工具类，提供视图层常用的值格式化方法。
 */
public class FormatUtils {

	private FormatUtils() {
	}

	/**
	 * 将字节数格式化为人类可读的文件大小字符串。
	 *
	 * @param bytes 文件大小（字节）
	 * @return 格式化后的字符串，如 "512 B"、"1.5 KB"、"3.2 MB"、"1.05 GB"
	 */
	public static String formatFileSize(long bytes) {
		if (bytes < 1024) return bytes + " B";
		if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
		if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
		return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
	}
}
