package io.github.xuse.romking.nointro;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.nointro.NoIntroDatParser.DatFile;
import io.github.xuse.romking.nointro.NoIntroDatParser.DatGame;
import io.github.xuse.romking.nointro.NoIntroDatParser.DatRom;
import io.github.xuse.romking.repo.dal.KnownRomRepository;
import io.github.xuse.romking.repo.enums.Region;
import io.github.xuse.romking.repo.obj.KnownRom;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * No-Intro DAT文件导入服务。
 * 支持导入标准DAT和P/C DAT，将ROM信息存入known_rom表。
 * 
 * 设计要点：
 * 1. 批量插入（batch）提升性能
 * 2. MD5重复静默跳过（同一ROM文件在多个game条目中出现是正常的）
 * 3. 增量导入：按platform+datVersion判断是否已导入过
 * 4. 单条错误不中断整体任务
 */
@Slf4j
@Service
public class DatImportService {

	@Inject
	private KnownRomRepository knownRomRepo;

	private final NoIntroDatParser parser = new NoIntroDatParser();

	/** 查询已存在MD5时的分批大小 */
	private static final int QUERY_BATCH_SIZE = 500;

	// No-Intro DAT名称到Platform的映射
	private static final Map<String, Platform> PLATFORM_MAP = new HashMap<>();
	static {
		// Nintendo
		PLATFORM_MAP.put("Nintendo - Nintendo Entertainment System", Platform.NES);
		PLATFORM_MAP.put("Nintendo - Super Nintendo Entertainment System", Platform.SNES);
		PLATFORM_MAP.put("Nintendo - Nintendo 64", Platform.N64);
		PLATFORM_MAP.put("Nintendo - GameCube", Platform.NGC);
		PLATFORM_MAP.put("Nintendo - Nintendo GameCube", Platform.NGC);
		PLATFORM_MAP.put("Nintendo - Wii", Platform.Wii);
		PLATFORM_MAP.put("Nintendo - Game Boy", Platform.GB);
		PLATFORM_MAP.put("Nintendo - Game Boy Color", Platform.GBC);
		PLATFORM_MAP.put("Nintendo - Game Boy Advance", Platform.GBA);
		PLATFORM_MAP.put("Nintendo - Nintendo DS", Platform.NDS);
		PLATFORM_MAP.put("Nintendo - Nintendo 3DS", Platform.n3DS);
		PLATFORM_MAP.put("Nintendo - New Nintendo 3DS", Platform.n3DS);
		PLATFORM_MAP.put("Nintendo - Virtual Boy", Platform.VirtualBoy);
		PLATFORM_MAP.put("Nintendo - Family Computer Disk System", Platform.NES);
		// Sega
		PLATFORM_MAP.put("Sega - Mega Drive - Genesis", Platform.MD);
		PLATFORM_MAP.put("Sega - Mega Drive", Platform.MD);
		PLATFORM_MAP.put("Sega - Genesis", Platform.MD);
		PLATFORM_MAP.put("Sega - 32X", Platform.MD);
		PLATFORM_MAP.put("Sega - Game Gear", Platform.MD);
		PLATFORM_MAP.put("Sega - Master System - Mark III", Platform.MD);
		PLATFORM_MAP.put("Sega - Saturn", Platform.SS);
		PLATFORM_MAP.put("Sega - Sega Saturn", Platform.SS);
		PLATFORM_MAP.put("Sega - Dreamcast", Platform.DC);
		PLATFORM_MAP.put("Sega - Sega Mega CD + Sega CD", Platform.MD);
		// Sony
		PLATFORM_MAP.put("Sony - PlayStation", Platform.PS);
		PLATFORM_MAP.put("Sony - PlayStation 2", Platform.PS2);
		PLATFORM_MAP.put("Sony - PlayStation Portable", Platform.PSP);
		PLATFORM_MAP.put("Sony - PlayStation Vita", Platform.PSV);
		// Non-Redump 前缀
		PLATFORM_MAP.put("Non-Redump - Sega - Sega Saturn", Platform.SS);
		PLATFORM_MAP.put("Non-Redump - Sega - Dreamcast", Platform.DC);
		PLATFORM_MAP.put("Non-Redump - Sega - Sega Mega CD + Sega CD", Platform.MD);
		PLATFORM_MAP.put("Non-Redump - Sony - PlayStation", Platform.PS);
		PLATFORM_MAP.put("Non-Redump - Sony - PlayStation 2", Platform.PS2);
		PLATFORM_MAP.put("Non-Redump - Sony - PlayStation Portable", Platform.PSP);
		PLATFORM_MAP.put("Non-Redump - Nintendo - Nintendo GameCube", Platform.NGC);
		PLATFORM_MAP.put("Non-Redump - Nintendo - Wii", Platform.Wii);
	}

	/**
	 * 导入标准DAT文件（批量模式）。
	 * 
	 * @param datFile DAT文件路径
	 * @param platform 平台（如果为null则从DAT header推断）
	 * @return 导入的条目数
	 */
	public int importDat(File datFile, Platform platform) {
		DatFile dat = parser.parse(datFile);
		if (platform == null) {
			platform = guessPlatform(dat.getName());
		}
		if (platform == null) {
			log.warn("无法识别平台: {}, 跳过", dat.getName());
			return 0;
		}

		String datVersion = dat.getVersion();

		// 增量判断：如果该平台+版本已导入过，跳过
		if (isAlreadyImported(platform, datVersion)) {
			log.info("已导入过，跳过: {} - 平台={}, 版本={}", dat.getName(), platform, datVersion);
			return 0;
		}

		// 收集所有待插入记录
		List<KnownRom> batch = new ArrayList<>();
		int totalRoms = 0;

		for (DatGame game : dat.getGames()) {
			for (DatRom rom : game.getRoms()) {
				totalRoms++;
				String md5 = toLower(rom.getMd5());
				// 跳过无MD5的条目（无法用于识别）
				if (md5 == null) {
					continue;
				}

				KnownRom known = new KnownRom();
				known.setGameName(game.getName());
				known.setRomFileName(rom.getName());
				known.setMd5(md5);
				known.setCrc(toLower(rom.getCrc()));
				known.setSha1(toLower(rom.getSha1()));
				known.setRomSize(rom.getSize());
				known.setPlatform(platform);
				known.setRegion(guessRegion(game.getName()));
				known.setSource("No-Intro");
				known.setDatVersion(datVersion);
				known.setParentName(game.getCloneOf());
				batch.add(known);
			}
		}

		// 批量插入，忽略重复
		int inserted = batchInsertIgnoreDuplicate(batch);
		log.info("导入完成: {} - 平台={}, 插入{}条, 跳过重复{}条, 总ROM条目{}",
				dat.getName(), platform, inserted, batch.size() - inserted, totalRoms);
		return inserted;
	}

	/**
	 * 导入P/C DAT文件，补充parent/clone关系。
	 * 应在标准DAT导入之后调用。
	 * 
	 * @param datFile P/C DAT文件路径
	 * @param platform 平台
	 * @return 更新的条目数
	 */
	public int importParentClone(File datFile, Platform platform) {
		DatFile dat = parser.parse(datFile);
		if (platform == null) {
			platform = guessPlatform(dat.getName());
		}
		if (platform == null) {
			log.warn("无法识别平台: {}, 跳过P/C", dat.getName());
			return 0;
		}

		int updated = 0;
		int errors = 0;

		for (DatGame game : dat.getGames()) {
			String parentName = game.getCloneOf();
			if (parentName == null || parentName.isEmpty()) {
				parentName = game.getName();
			}

			for (DatRom rom : game.getRoms()) {
				String md5 = toLower(rom.getMd5());
				if (md5 == null) continue;

				try {
					long cnt = knownRomRepo.getFactory().update(KnownRomRepository.t)
							.set(KnownRomRepository.parentName, parentName)
							.where(KnownRomRepository.md5.eq(md5))
							.execute();
					updated += (int) cnt;
				} catch (Exception e) {
					errors++;
					if (errors <= 5) {
						log.warn("P/C更新失败: md5={}, error={}", md5, e.getMessage());
					}
				}
			}
		}

		if (errors > 0) {
			log.warn("P/C关系导入完成: {} - 更新{}条, 错误{}条", dat.getName(), updated, errors);
		} else {
			log.info("P/C关系导入完成: {} - 更新{}条", dat.getName(), updated);
		}
		return updated;
	}

	/**
	 * 批量导入目录下所有DAT文件（含ZIP压缩包）。
	 * 
	 * @param directory 包含DAT/ZIP文件的目录
	 * @param platforms 要导入的平台集合（null表示全部）
	 * @return 总导入条目数
	 */
	public int importDirectory(File directory, Set<Platform> platforms) {
		return importDirectory(directory, platforms, null);
	}

	/**
	 * 批量导入目录下所有DAT文件（含ZIP压缩包），带进度回调。
	 * 
	 * @param directory 包含DAT/ZIP文件的目录
	 * @param platforms 要导入的平台集合（null表示全部）
	 * @param progressCallback 进度回调（可为null）
	 * @return 总导入条目数
	 */
	public int importDirectory(File directory, Set<Platform> platforms, java.util.function.Consumer<String> progressCallback) {
		if (!directory.isDirectory()) {
			log.error("不是目录: {}", directory.getAbsolutePath());
			return 0;
		}

		File[] files = directory.listFiles((dir, name) -> {
			String lower = name.toLowerCase();
			return lower.endsWith(".dat") || lower.endsWith(".xml") || lower.endsWith(".zip");
		});
		if (files == null || files.length == 0) {
			log.warn("目录下无DAT/ZIP文件: {}", directory.getAbsolutePath());
			return 0;
		}

		// 按文件名排序，确保标准DAT在P/C之前导入
		java.util.Arrays.sort(files, (a, b) -> {
			boolean aIsPC = a.getName().contains("Parent-Clone");
			boolean bIsPC = b.getName().contains("Parent-Clone");
			if (aIsPC != bIsPC) return aIsPC ? 1 : -1;
			return a.getName().compareTo(b.getName());
		});

		// 统计需要处理的文件数（排除跳过的）
		int totalFiles = 0;
		for (File file : files) {
			if (platforms == null || isFileNameMatchPlatforms(file.getName(), platforms)) {
				totalFiles++;
			}
		}

		int total = 0;
		int skipped = 0;
		int failed = 0;
		int processed = 0;
		for (File file : files) {
			String fileName = file.getName();

			// 平台过滤
			if (platforms != null && !isFileNameMatchPlatforms(fileName, platforms)) {
				log.debug("跳过（不在导入范围）: {}", fileName);
				skipped++;
				continue;
			}

			processed++;
			// 更新进度
			if (progressCallback != null) {
				progressCallback.accept(String.format("[%d/%d] %s (已导入%d条)",
						processed, totalFiles, fileName, total));
			}

			boolean isParentClone = fileName.contains("Parent-Clone");

			try {
				if (fileName.toLowerCase().endsWith(".zip")) {
					total += importZipFile(file, isParentClone);
				} else if (isParentClone) {
					total += importParentClone(file, null);
				} else {
					total += importDat(file, null);
				}
			} catch (Exception e) {
				failed++;
				log.error("导入失败（已跳过）: {} - {}", fileName, e.getMessage());
			}
		}
		log.info("目录导入完成: 导入{}条, 跳过{}个文件, 失败{}个文件", total, skipped, failed);
		return total;
	}

	/**
	 * 批量导入目录（兼容旧接口）。
	 */
	public int importDirectory(File directory, boolean filterByConfig) {
		Set<Platform> platforms = filterByConfig ? DatImportConfig.DEFAULT_PLATFORMS : null;
		return importDirectory(directory, platforms);
	}

	// ========== 内部方法 ==========

	/**
	 * 批量插入，跳过已存在的MD5记录。
	 * 先查出已存在的MD5集合，过滤后再插入，避免触发唯一约束异常。
	 */
	private int batchInsertIgnoreDuplicate(List<KnownRom> records) {
		if (records.isEmpty()) return 0;

		// 收集所有待插入的MD5
		List<String> allMd5s = new ArrayList<>(records.size());
		for (KnownRom r : records) {
			if (r.getMd5() != null) {
				allMd5s.add(r.getMd5());
			}
		}

		// 分批查询已存在的MD5（避免IN子句过长）
		Set<String> existingMd5s = new java.util.HashSet<>();
		for (int i = 0; i < allMd5s.size(); i += QUERY_BATCH_SIZE) {
			int end = Math.min(i + QUERY_BATCH_SIZE, allMd5s.size());
			List<String> chunk = allMd5s.subList(i, end);
			List<KnownRom> found = knownRomRepo.find(q ->
					q.where(KnownRomRepository.md5.in(chunk)));
			for (KnownRom kr : found) {
				if (kr.getMd5() != null) {
					existingMd5s.add(kr.getMd5());
				}
			}
		}

		// 过滤掉已存在的，只插入新记录
		int inserted = 0;
		int skipped = existingMd5s.size();
		for (KnownRom known : records) {
			if (known.getMd5() != null && existingMd5s.contains(known.getMd5())) {
				continue;
			}
			try {
				knownRomRepo.insert(known);
				inserted++;
			} catch (Exception e) {
				// 极少数情况：并发插入或同批次内MD5重复
				skipped++;
			}
		}

		if (skipped > 0) {
			log.debug("跳过{}条已存在记录", skipped);
		}
		return inserted;
	}

	/**
	 * 增量判断：检查该平台+版本是否已有导入记录。
	 * 如果known_rom表中已存在相同platform+datVersion的记录，视为已导入。
	 */
	private boolean isAlreadyImported(Platform platform, String datVersion) {
		if (datVersion == null || datVersion.isEmpty()) return false;

		List<KnownRom> existing = knownRomRepo.find(q -> q
				.where(KnownRomRepository.platform.eq(platform),
						KnownRomRepository.datVersion.eq(datVersion))
				.limit(1));
		return !existing.isEmpty();
	}

	/**
	 * 判断文件名是否匹配指定的平台集合
	 */
	private boolean isFileNameMatchPlatforms(String fileName, Set<Platform> platforms) {
		if (fileName == null) return false;
		// 先尝试从文件名推断平台
		Platform guessed = guessFileNamePlatform(fileName);
		if (guessed != null) {
			return platforms.contains(guessed);
		}
		// 无法推断时，用旧的关键词匹配逻辑兜底
		return DatImportConfig.isFileNameIncluded(fileName);
	}

	/**
	 * 从ZIP/DAT文件名推断平台（文件名格式：Platform Name (date).zip）
	 */
	private Platform guessFileNamePlatform(String fileName) {
		// 去掉日期和扩展名部分
		int parenIdx = fileName.indexOf('(');
		String prefix = parenIdx > 0 ? fileName.substring(0, parenIdx).trim() : fileName;
		// 去掉 "Parent-Clone" 后缀
		prefix = prefix.replace("(Parent-Clone)", "").trim();
		return PLATFORM_MAP.get(prefix);
	}

	/**
	 * 导入ZIP压缩的DAT文件。
	 */
	private int importZipFile(File zipFile, boolean isParentClone) {
		try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFile)) {
			java.util.zip.ZipEntry datEntry = zf.stream()
					.filter(e -> !e.isDirectory() && (e.getName().endsWith(".dat") || e.getName().endsWith(".xml")))
					.findFirst()
					.orElse(null);
			if (datEntry == null) {
				log.warn("ZIP内无DAT文件: {}", zipFile.getName());
				return 0;
			}

			File tempFile = File.createTempFile("nointro_", ".dat");
			try {
				try (java.io.InputStream is = zf.getInputStream(datEntry);
					 java.io.OutputStream os = new java.io.FileOutputStream(tempFile)) {
					is.transferTo(os);
				}
				if (isParentClone) {
					return importParentClone(tempFile, null);
				} else {
					return importDat(tempFile, null);
				}
			} finally {
				tempFile.delete();
			}
		} catch (java.io.IOException e) {
			log.error("读取ZIP失败: {}", zipFile.getName(), e);
			return 0;
		}
	}

	/**
	 * 从DAT名称推断平台
	 */
	Platform guessPlatform(String datName) {
		if (datName == null) return null;

		// 精确匹配
		Platform p = PLATFORM_MAP.get(datName);
		if (p != null) return p;

		// 模糊匹配
		String upper = datName.toUpperCase();
		for (Map.Entry<String, Platform> entry : PLATFORM_MAP.entrySet()) {
			if (upper.contains(entry.getKey().toUpperCase())) {
				return entry.getValue();
			}
		}

		// 尝试从名称中提取关键词
		if (upper.contains("NES") && !upper.contains("SNES")) return Platform.NES;
		if (upper.contains("SUPER NINTENDO") || upper.contains("SNES")) return Platform.SNES;
		if (upper.contains("NINTENDO 64") || upper.contains("N64")) return Platform.N64;
		if (upper.contains("GAME BOY ADVANCE") || upper.contains("GBA")) return Platform.GBA;
		if (upper.contains("GAME BOY COLOR") || upper.contains("GBC")) return Platform.GBC;
		if (upper.contains("GAME BOY") || upper.contains(" GB")) return Platform.GB;
		if (upper.contains("NINTENDO DS") || upper.contains("NDS")) return Platform.NDS;
		if (upper.contains("MEGA DRIVE") || upper.contains("GENESIS")) return Platform.MD;
		if (upper.contains("SATURN")) return Platform.SS;
		if (upper.contains("DREAMCAST")) return Platform.DC;
		if (upper.contains("PLAYSTATION PORTABLE") || upper.contains("PSP")) return Platform.PSP;
		if (upper.contains("PLAYSTATION 2") || upper.contains("PS2")) return Platform.PS2;
		if (upper.contains("PLAYSTATION")) return Platform.PS;
		if (upper.contains("GAMECUBE")) return Platform.NGC;

		return null;
	}

	/**
	 * 从No-Intro游戏名中解析区域
	 */
	private static final Pattern REGION_PATTERN = Pattern.compile("\\(([^)]+)\\)");

	private Region guessRegion(String gameName) {
		if (gameName == null) return Region.OTHER;

		Matcher m = REGION_PATTERN.matcher(gameName);
		while (m.find()) {
			String tag = m.group(1).toUpperCase();
			if (tag.contains("JAPAN") || tag.equals("JPN") || tag.equals("J")) return Region.JPN;
			if (tag.contains("USA") || tag.equals("US") || tag.equals("U")) return Region.USA;
			if (tag.contains("EUROPE") || tag.equals("EUR") || tag.equals("E")) return Region.EUR;
			if (tag.contains("CHINA") || tag.equals("CHN") || tag.contains("CHINESE")) return Region.CHN;
			if (tag.contains("KOREA") || tag.equals("KOR") || tag.equals("K")) return Region.KOR;
			if (tag.contains("ASIA")) return Region.ASA;
		}
		return Region.OTHER;
	}

	private String toLower(String s) {
		return s == null || s.isEmpty() ? null : s.toLowerCase();
	}
}
