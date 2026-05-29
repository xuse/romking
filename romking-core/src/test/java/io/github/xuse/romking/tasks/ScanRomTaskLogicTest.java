package io.github.xuse.romking.tasks;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.enums.Region;

/**
 * ScanRomTask 纯逻辑单元测试。
 * 测试平台推断、区域猜测等不依赖数据库的逻辑。
 * 通过反射调用私有方法进行测试。
 */
class ScanRomTaskLogicTest {

	private static Method guessPlatformMethod;
	private static Method guessRegionMethod;
	private static Object scanTask;

	@BeforeAll
	static void setup() throws Exception {
		// 通过反射获取私有方法
		guessPlatformMethod = ScanRomTask.class.getDeclaredMethod("guessPlatform", String.class);
		guessPlatformMethod.setAccessible(true);

		guessRegionMethod = ScanRomTask.class.getDeclaredMethod("guessRegion", String.class);
		guessRegionMethod.setAccessible(true);

		// 创建一个最小化的 ScanRomTask 实例（仅用于调用实例方法）
		// 使用 Unsafe 或 objenesis 可以绕过构造函数，但这里用 null 参数的方式
		// 由于构造函数有 Assert 检查，我们用反射绕过
		var constructor = ScanRomTask.class.getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		// 构造函数需要 File 参数且会 assert isDirectory，所以我们用临时目录
		java.io.File tempDir = java.nio.file.Files.createTempDirectory("scan-test").toFile();
		var options = new io.github.xuse.romking.service.RomScanOptions();
		options.setLabel("test");
		scanTask = constructor.newInstance(tempDir, options, null, null, null, null, null);
		tempDir.delete();
	}

	// ========== 平台推断测试 ==========

	@ParameterizedTest
	@CsvSource({
			"gba, GBA",
			"GBA, GBA",
			"nes, NES",
			"NES, NES",
			"snes, SNES",
			"n64, N64",
			"gb, GB",
			"gbc, GBC",
			"nds, NDS",
			"md, MD",
			"ss, SS",
			"dc, DC",
			"ps, PS",
			"ps2, PS2",
			"psp, PSP",
	})
	void testGuessPlatform_directMatch(String dirName, String expectedPlatform) throws Exception {
		Platform result = (Platform) guessPlatformMethod.invoke(scanTask, dirName);
		assertEquals(Platform.valueOf(expectedPlatform), result,
				"目录名 '" + dirName + "' 应识别为 " + expectedPlatform);
	}

	@ParameterizedTest
	@CsvSource({
			"FC, NES",
			"famicom, NES",
			"SFC, SNES",
			"SuperNES, SNES",
			"SuperFamicom, SNES",
			"SuperNintendo, SNES",
			"Nintendo64, N64",
			"GameCube, NGC",
			"GameBoy, GB",
			"GameBoyColor, GBC",
			"GameBoyAdvance, GBA",
			"NintendoDS, NDS",
			"MegaDrive, MD",
			"Genesis, MD",
			"Saturn, SS",
			"Dreamcast, DC",
			"PlayStation, PS",
			"PS1, PS",
			"PSX, PS",
			"PlayStation2, PS2",
	})
	void testGuessPlatform_aliases(String dirName, String expectedPlatform) throws Exception {
		Platform result = (Platform) guessPlatformMethod.invoke(scanTask, dirName);
		assertEquals(Platform.valueOf(expectedPlatform), result,
				"别名 '" + dirName + "' 应识别为 " + expectedPlatform);
	}

	@Test
	void testGuessPlatform_unknownReturnsNull() throws Exception {
		Platform result = (Platform) guessPlatformMethod.invoke(scanTask, "unknown_platform");
		assertNull(result, "无法识别的目录名应返回null");
	}

	// ========== 区域猜测测试 ==========

	@ParameterizedTest
	@CsvSource({
			"'Super Mario (J)', JPN",
			"'Super Mario (JPN)', JPN",
			"'Super Mario (Japan)', JPN",
			"'Super Mario (U)', USA",
			"'Super Mario (USA)', USA",
			"'Super Mario (E)', EUR",
			"'Super Mario (EUR)', EUR",
			"'Super Mario (Europe)', EUR",
			"'Super Mario (K)', KOR",
			"'Super Mario (KOR)', KOR",
			"'Super Mario (Korea)', KOR",
			"'Super Mario (CN)', CHN",
			"'Super Mario (CHN)', CHN",
	})
	void testGuessRegion_fromParentheses(String fileName, String expectedRegion) throws Exception {
		Region result = (Region) guessRegionMethod.invoke(scanTask, fileName);
		assertEquals(Region.valueOf(expectedRegion), result,
				"文件名 '" + fileName + "' 应识别区域为 " + expectedRegion);
	}

	@Test
	void testGuessRegion_chineseCharacters() throws Exception {
		Region result = (Region) guessRegionMethod.invoke(scanTask, "超级马里奥");
		assertEquals(Region.CHN, result, "包含中文字符应识别为CHN");
	}

	@Test
	void testGuessRegion_unknownReturnsOther() throws Exception {
		Region result = (Region) guessRegionMethod.invoke(scanTask, "Super Mario Bros");
		assertEquals(Region.OTHER, result, "无法识别区域应返回OTHER");
	}

	@Test
	void testGuessRegion_nullReturnsOther() throws Exception {
		Region result = (Region) guessRegionMethod.invoke(scanTask, (Object) null);
		assertEquals(Region.OTHER, result, "null应返回OTHER");
	}
}
