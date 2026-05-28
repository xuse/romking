package io.github.xuse.romking.nointro;

import java.util.EnumSet;
import java.util.Set;

import io.github.xuse.romking.core.Platform;

/**
 * DAT导入配置。
 * 定义日常导入时关注的平台范围，不在范围内的DAT文件跳过。
 */
public class DatImportConfig {

	/**
	 * 默认导入的平台集合。
	 * 
	 * SEGA全部: MD, SS, DC
	 * Nintendo: NES, SNES, GBA, n3DS, Wii
	 * Sony: PS, PS2, PSP
	 */
	public static final Set<Platform> DEFAULT_PLATFORMS = EnumSet.of(
			// SEGA 全部
			Platform.MD,
			Platform.SS,
			Platform.DC,
			// Nintendo 选定
			Platform.NES,
			Platform.SNES,
			Platform.GBA,
			Platform.n3DS,
			Platform.Wii,
			// Sony 选定
			Platform.PS,
			Platform.PS2,
			Platform.PSP
	);

	/**
	 * 文件名中包含以下关键词的，即使平台无法精确匹配到 Platform 枚举，
	 * 也视为属于关注范围（用于匹配 Sega 的各种子平台）。
	 */
	public static final String[] FILENAME_INCLUDE_KEYWORDS = {
			"Sega -",
			"Sega -",
	};

	/**
	 * 判断给定平台是否在默认导入范围内
	 */
	public static boolean isIncluded(Platform platform) {
		return platform != null && DEFAULT_PLATFORMS.contains(platform);
	}

	/**
	 * 根据文件名判断是否应该导入（用于平台无法精确识别时的兜底）。
	 * 文件名包含 "Sega" 的一律导入。
	 */
	public static boolean isFileNameIncluded(String fileName) {
		if (fileName == null) return false;
		String lower = fileName.toLowerCase();
		// Sega 全系列都导入
		if (lower.contains("sega")) return true;
		// Nintendo 指定平台关键词
		if (lower.contains("entertainment system")) return true; // NES
		if (lower.contains("super nintendo")) return true; // SNES
		if (lower.contains("game boy advance") && !lower.contains("video") && !lower.contains("e-reader")) return true;
		if (lower.contains("nintendo 3ds")) return true;
		if (lower.contains("wii") && !lower.contains("wii u")) return true;
		// Sony 指定平台关键词
		if (lower.contains("playstation") && !lower.contains("3") && !lower.contains("vita") && !lower.contains("mobile")) return true;
		if (lower.contains("playstation 2")) return true;
		if (lower.contains("playstation portable")) return true;
		return false;
	}
}
