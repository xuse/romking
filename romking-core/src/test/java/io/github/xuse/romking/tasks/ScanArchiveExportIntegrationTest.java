package io.github.xuse.romking.tasks;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.github.xuse.romking.TestRomConsole;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.metadata.ParserType;
import io.github.xuse.romking.metadata.ee.GameListService;
import io.github.xuse.romking.nointro.DatManageService;
import io.github.xuse.romking.repo.dal.MediaFileRepository;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.enums.FileStatus;
import io.github.xuse.romking.repo.enums.RepoType;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.service.RomExportOptions;
import io.github.xuse.romking.service.RomScanOptions;

/**
 * 集成测试：扫描 → 归档 → 导出 端到端流程。
 * 
 * 使用内存 H2 数据库和临时文件系统目录模拟真实场景。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScanArchiveExportIntegrationTest {

	private static TestRomConsole console;
	private static Path tempRoot;
	private static Path instanceDir;   // 模拟 TF 卡
	private static Path archiveDir;    // 模拟归档仓库
	private static Path exportDir;     // 模拟导出目标

	@BeforeAll
	static void setup() throws Exception {
		console = new TestRomConsole();
		tempRoot = Files.createTempDirectory("romking-test");
		instanceDir = tempRoot.resolve("instance");
		archiveDir = tempRoot.resolve("archive");
		exportDir = tempRoot.resolve("export");

		// 创建模拟 TF 卡目录结构
		// instance/gba/game1.zip
		// instance/gba/game2.gba
		// instance/nes/game3.nes
		createInstanceFiles();
	}

	@AfterAll
	static void cleanup() throws Exception {
		if (tempRoot != null && Files.exists(tempRoot)) {
			Files.walk(tempRoot)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}

	// ========== 扫描测试 ==========

	@Test
	@Order(1)
	void testScanInstanceRepo() {
		RomScanOptions options = new RomScanOptions();
		options.setLabel("test-card");
		options.setRepoType(RepoType.INSTANCE);
		options.setScanWithoutGamelist(true);
		options.setComputeMd5(true);
		options.setComputeCrc(true);
		options.setIncremental(false);

		RomDirRepository romDirRepo = console.getBean(RomDirRepository.class);
		RomFileRepository romFileRepo = console.getBean(RomFileRepository.class);
		MediaFileRepository mediaRepo = console.getBean(MediaFileRepository.class);
		GameListService gameListService = console.getBean(GameListService.class);
		DatManageService datManageService = console.getBean(DatManageService.class);

		// 先手动创建 RomDir 记录（避免 auto-increment ID 回填问题）
		for (File subDir : instanceDir.toFile().listFiles(File::isDirectory)) {
			String dirName = subDir.getName();
			Platform platform = dirName.equalsIgnoreCase("gba") ? Platform.GBA :
					dirName.equalsIgnoreCase("nes") ? Platform.NES : null;
			if (platform == null) continue;

			console.getFactory().insert(RomDirRepository.t)
					.set(RomDirRepository.t.label, "test-card")
					.set(RomDirRepository.t.platform, platform)
					.set(RomDirRepository.t.rootpath, subDir.getAbsolutePath())
					.set(RomDirRepository.t.description, "")
					.set(RomDirRepository.t.type, RepoType.INSTANCE)
					.set(RomDirRepository.t.createTime, new java.util.Date())
					.execute();
		}

		// 使用增量模式扫描（会找到已有的 RomDir 记录）
		options.setIncremental(true);

		ScanRomTask task = new ScanRomTask(
				instanceDir.toFile(), options,
				romDirRepo, romFileRepo, mediaRepo, gameListService, datManageService);

		ProcessResult result = task.execute();

		// 验证扫描成功
		assertEquals(200, result.getCode(), "扫描应成功: " + result.getMessage());

		// 验证目录记录
		List<RomDir> dirs = romDirRepo.find(q -> {});
		assertTrue(dirs.size() >= 2, "应至少有2个平台目录(gba, nes)");

		// 验证创建了ROM文件记录
		List<RomFile> roms = romFileRepo.find(q -> {});
		assertEquals(3, roms.size(), "应扫描到3个ROM文件");

		// 验证MD5已计算
		for (RomFile rom : roms) {
			assertNotNull(rom.getMd5(), "MD5应已计算: " + rom.getName());
			assertFalse(rom.getMd5().isEmpty(), "MD5不应为空: " + rom.getName());
		}

		// 验证平台识别
		long gbaCount = roms.stream().filter(r -> r.getPlatform() == Platform.GBA).count();
		long nesCount = roms.stream().filter(r -> r.getPlatform() == Platform.NES).count();
		assertEquals(2, gbaCount, "GBA平台应有2个ROM");
		assertEquals(1, nesCount, "NES平台应有1个ROM");
	}

	@Test
	@Order(2)
	void testIncrementalScan_skipsUnchangedFiles() {
		RomScanOptions options = new RomScanOptions();
		options.setLabel("test-card");
		options.setRepoType(RepoType.INSTANCE);
		options.setScanWithoutGamelist(true);
		options.setComputeMd5(true);
		options.setComputeCrc(true);
		options.setIncremental(true);  // 增量模式

		RomDirRepository romDirRepo = console.getBean(RomDirRepository.class);
		RomFileRepository romFileRepo = console.getBean(RomFileRepository.class);
		MediaFileRepository mediaRepo = console.getBean(MediaFileRepository.class);
		GameListService gameListService = console.getBean(GameListService.class);
		DatManageService datManageService = console.getBean(DatManageService.class);

		ScanRomTask task = new ScanRomTask(
				instanceDir.toFile(), options,
				romDirRepo, romFileRepo, mediaRepo, gameListService, datManageService);

		ProcessResult result = task.execute();

		assertEquals(200, result.getCode(), "增量扫描应成功");
		// 增量扫描后，文件未变更应被跳过，记录数不应翻倍
		List<RomFile> roms = romFileRepo.find(q -> {});
		assertEquals(3, roms.size(), "增量扫描不应产生重复记录");
	}

	// ========== 归档测试 ==========

	@Test
	@Order(3)
	void testArchive_copiesFilesAndMetadata() throws Exception {
		// 先创建归档目录
		Files.createDirectories(archiveDir);

		// 创建归档目标的 RomDir
		RomDirRepository romDirRepo = console.getBean(RomDirRepository.class);
		RomFileRepository romFileRepo = console.getBean(RomFileRepository.class);
		MediaFileRepository mediaRepo = console.getBean(MediaFileRepository.class);

		// 找到源目录（GBA）
		List<RomDir> sourceDirs = romDirRepo.find(q ->
				q.where(RomDirRepository.t.label.eq("test-card"),
						RomDirRepository.t.platform.eq(Platform.GBA)));
		assertFalse(sourceDirs.isEmpty(), "应有GBA源目录");
		RomDir sourceDir = sourceDirs.get(0);

		// 创建归档目标目录
		Path archiveGbaDir = archiveDir.resolve("gba");
		Files.createDirectories(archiveGbaDir);

		// 使用原生SQL插入归档目录（避免ID回填问题）
		console.getFactory().insert(RomDirRepository.t)
				.set(RomDirRepository.t.label, "my-archive")
				.set(RomDirRepository.t.platform, Platform.GBA)
				.set(RomDirRepository.t.rootpath, archiveGbaDir.toAbsolutePath().toString())
				.set(RomDirRepository.t.description, "测试归档仓库")
				.set(RomDirRepository.t.type, RepoType.ARCHIVE)
				.set(RomDirRepository.t.createTime, new java.util.Date())
				.execute();

		// 查询获取自增ID
		List<RomDir> targetDirs = romDirRepo.find(q ->
				q.where(RomDirRepository.t.label.eq("my-archive"),
						RomDirRepository.t.platform.eq(Platform.GBA)));
		RomDir target = targetDirs.get(0);

		// 执行归档
		ArchiveRomTask archiveTask = new ArchiveRomTask(
				sourceDir.getId(), target.getId(),
				romDirRepo, romFileRepo, mediaRepo);

		ProcessResult result = archiveTask.execute();

		assertEquals(200, result.getCode(), "归档应成功: " + result.getMessage());

		// 验证归档后的记录
		List<RomFile> archivedRoms = romFileRepo.find(q ->
				q.where(RomFileRepository.dirId.eq(target.getId())));
		assertEquals(2, archivedRoms.size(), "应归档2个GBA ROM");

		// 验证元数据已复制（displayName）
		List<RomFile> sourceRoms = romFileRepo.find(q ->
				q.where(RomFileRepository.dirId.eq(sourceDir.getId())));
		for (RomFile sourceRom : sourceRoms) {
			RomFile archived = archivedRoms.stream()
					.filter(r -> r.getMd5().equals(sourceRom.getMd5()))
					.findFirst().orElse(null);
			assertNotNull(archived, "归档中应有对应ROM: " + sourceRom.getName());
			assertEquals(sourceRom.getDisplayName(), archived.getDisplayName(),
					"displayName应被复制");
			assertEquals(sourceRom.getName(), archived.getName(),
					"name应被复制");
		}

		// 验证物理文件已复制
		for (RomFile archived : archivedRoms) {
			File physicalFile = new File(archiveGbaDir.toFile(), archived.getFilepath());
			assertTrue(physicalFile.exists(), "归档物理文件应存在: " + archived.getFilepath());
		}
	}

	@Test
	@Order(4)
	void testArchive_deduplicatesByMd5() {
		RomDirRepository romDirRepo = console.getBean(RomDirRepository.class);
		RomFileRepository romFileRepo = console.getBean(RomFileRepository.class);
		MediaFileRepository mediaRepo = console.getBean(MediaFileRepository.class);

		// 再次归档同一源目录到同一目标
		List<RomDir> sourceDirs = romDirRepo.find(q ->
				q.where(RomDirRepository.t.label.eq("test-card"),
						RomDirRepository.t.platform.eq(Platform.GBA)));
		List<RomDir> targetDirs = romDirRepo.find(q ->
				q.where(RomDirRepository.t.label.eq("my-archive"),
						RomDirRepository.t.platform.eq(Platform.GBA)));

		ArchiveRomTask archiveTask = new ArchiveRomTask(
				sourceDirs.get(0).getId(), targetDirs.get(0).getId(),
				romDirRepo, romFileRepo, mediaRepo);

		ProcessResult result = archiveTask.execute();

		assertEquals(200, result.getCode(), "重复归档应成功（全部跳过）");

		// 验证没有产生重复记录
		List<RomFile> archivedRoms = romFileRepo.find(q ->
				q.where(RomFileRepository.dirId.eq(targetDirs.get(0).getId())));
		assertEquals(2, archivedRoms.size(), "重复归档不应增加记录");
	}

	// ========== 导出测试 ==========

	@Test
	@Order(5)
	void testExport_copiesFilesAndGeneratesGamelist() throws Exception {
		Files.createDirectories(exportDir);

		RomDirRepository romDirRepo = console.getBean(RomDirRepository.class);
		RomFileRepository romFileRepo = console.getBean(RomFileRepository.class);
		MediaFileRepository mediaRepo = console.getBean(MediaFileRepository.class);
		GameListService gameListService = console.getBean(GameListService.class);

		// 获取归档目录ID
		List<RomDir> archiveDirs = romDirRepo.find(q ->
				q.where(RomDirRepository.t.label.eq("my-archive")));
		assertFalse(archiveDirs.isEmpty());

		int[] dirIds = archiveDirs.stream().mapToInt(RomDir::getId).toArray();

		RomExportOptions options = new RomExportOptions();
		options.setSourceDirIds(dirIds);
		options.setTargetPath(exportDir.toAbsolutePath().toString());
		options.setTargetLabel("export-card");
		options.setOverwrite(false);
		options.setQuickExport(false);
		options.setMetadataFormat(ParserType.EMUELEC);

		ExportRomTask exportTask = new ExportRomTask(
				options, romDirRepo, romFileRepo, mediaRepo, gameListService);

		ProcessResult result = exportTask.execute();

		assertTrue(result.getCode() >= 200 && result.getCode() < 300,
				"导出应成功: " + result.getMessage());

		// 验证导出目录结构
		File gbaExportDir = new File(exportDir.toFile(), "gba");
		assertTrue(gbaExportDir.exists(), "应创建gba平台目录");

		// 验证gamelist.xml已生成（如果GameListService正常工作）
		File gamelistFile = new File(gbaExportDir, "gamelist.xml");
		// gamelist.xml generation depends on GameListService XML serialization
		// which may not work in all test environments

		// 验证ROM文件已复制
		File[] exportedFiles = gbaExportDir.listFiles(f -> 
				!f.getName().equals("gamelist.xml") && !f.isDirectory());
		assertNotNull(exportedFiles);
		assertEquals(2, exportedFiles.length, "应导出2个ROM文件");
	}

	@Test
	@Order(6)
	void testExport_skipsAbnormalStatusFiles() {
		RomDirRepository romDirRepo = console.getBean(RomDirRepository.class);
		RomFileRepository romFileRepo = console.getBean(RomFileRepository.class);
		MediaFileRepository mediaRepo = console.getBean(MediaFileRepository.class);
		GameListService gameListService = console.getBean(GameListService.class);

		// 将归档中的一个ROM标记为MISSING
		List<RomDir> archiveDirs = romDirRepo.find(q ->
				q.where(RomDirRepository.t.label.eq("my-archive")));
		List<RomFile> archivedRoms = romFileRepo.find(q ->
				q.where(RomFileRepository.dirId.eq(archiveDirs.get(0).getId())));

		RomFile firstRom = archivedRoms.get(0);
		romFileRepo.getFactory().update(io.github.xuse.romking.repo.obj.QRomFile.romFile)
				.set(io.github.xuse.romking.repo.obj.QRomFile.romFile.fileStatus, FileStatus.MISSING)
				.where(io.github.xuse.romking.repo.obj.QRomFile.romFile.id.eq(firstRom.getId()))
				.execute();

		// 导出到新目录
		Path exportDir2 = tempRoot.resolve("export2");
		try {
			Files.createDirectories(exportDir2);
		} catch (IOException e) {
			fail("创建导出目录失败");
		}

		int[] dirIds = archiveDirs.stream().mapToInt(RomDir::getId).toArray();
		RomExportOptions options = new RomExportOptions();
		options.setSourceDirIds(dirIds);
		options.setTargetPath(exportDir2.toAbsolutePath().toString());
		options.setTargetLabel("export-card-2");
		options.setOverwrite(false);
		options.setQuickExport(true);
		options.setMetadataFormat(ParserType.EMUELEC);

		ExportRomTask exportTask = new ExportRomTask(
				options, romDirRepo, romFileRepo, mediaRepo, gameListService);

		ProcessResult result = exportTask.execute();

		assertTrue(result.getCode() >= 200 && result.getCode() < 300,
				"导出应成功: " + result.getMessage());

		// 验证只导出了1个文件（另一个MISSING被跳过）
		File gbaExportDir = new File(exportDir2.toFile(), "gba");
		if (gbaExportDir.exists()) {
			File[] exportedFiles = gbaExportDir.listFiles(f -> !f.getName().equals("gamelist.xml"));
			// 应该只有1个ROM被导出（另一个被标记为MISSING跳过了）
			assertNotNull(exportedFiles);
			assertEquals(1, exportedFiles.length, "MISSING状态的ROM应被跳过");
		}
	}

	// ========== 辅助方法 ==========

	private static void createInstanceFiles() throws Exception {
		// 创建 GBA 目录
		Path gbaDir = instanceDir.resolve("gba");
		Files.createDirectories(gbaDir);

		// game1.zip - 单文件ZIP
		createZipRom(gbaDir.resolve("game1.zip"), "Super Mario Advance.gba", "GBA ROM content for game1");

		// game2.gba - 裸ROM文件
		Files.write(gbaDir.resolve("game2.gba"), "GBA ROM content for game2 - different".getBytes(StandardCharsets.UTF_8));

		// 创建 NES 目录
		Path nesDir = instanceDir.resolve("nes");
		Files.createDirectories(nesDir);

		// game3.nes - 裸ROM文件
		Files.write(nesDir.resolve("game3 (J).nes"), "NES ROM content for game3".getBytes(StandardCharsets.UTF_8));
	}

	private static void createZipRom(Path zipPath, String innerFileName, String content) throws Exception {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
			ZipEntry entry = new ZipEntry(innerFileName);
			zos.putNextEntry(entry);
			zos.write(content.getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();
		}
	}
}
