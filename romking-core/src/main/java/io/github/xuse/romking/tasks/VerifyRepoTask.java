package io.github.xuse.romking.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.enums.FileStatus;
import io.github.xuse.romking.repo.enums.WrapType;
import io.github.xuse.romking.repo.obj.QRomFile;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.obj.RomFile;
import lombok.extern.slf4j.Slf4j;

/**
 * ROM仓库校验任务。
 * 检查仓库中每个ROM文件的存在性和完整性，更新fileStatus字段。
 * 
 * 支持两种校验模式：
 * - 快速模式（quickMode=true）：仅检查文件存在性 + ZIP文件的CRC校验（从ZIP头直接读取，无需解压）
 * - 完整模式（quickMode=false）：检查文件存在性 + MD5完整校验（需解压ZIP并计算）
 * 
 * 流程：
 * 1. 预检阶段：快速检查文件存在性，统计缺失率
 * 2. 如果缺失率>70%，暂停等待用户确认
 * 3. 正式校验：逐条检查文件状态
 * 4. 更新fileStatus字段
 */
@Slf4j
public class VerifyRepoTask implements Task {

	private final int dirId;
	private final RomDirRepository romDirRepo;
	private final RomFileRepository romFileRepo;
	private final boolean skipPreCheck;
	private final boolean quickMode;

	private long begin;
	private volatile TaskProgress taskProgress = new TaskProgress("等待开始", 0, 0);

	// 预检结果
	private boolean preCheckPassed = false;
	private float missingRate = 0;

	// 校验统计
	private int totalRoms = 0;
	private int okCount = 0;
	private int missingCount = 0;
	private int corruptedCount = 0;
	private int noChecksumCount = 0;
	private final List<String> details = new ArrayList<>();

	/**
	 * @param dirId 要校验的目录ID
	 * @param skipPreCheck 是否跳过预检（用户已确认继续时为true）
	 * @param quickMode 快速模式：ZIP文件用CRC校验（从ZIP头直接读取），非ZIP文件仅检查存在性
	 */
	public VerifyRepoTask(int dirId, RomDirRepository romDirRepo,
			RomFileRepository romFileRepo, boolean skipPreCheck, boolean quickMode) {
		this.dirId = dirId;
		this.romDirRepo = romDirRepo;
		this.romFileRepo = romFileRepo;
		this.skipPreCheck = skipPreCheck;
		this.quickMode = quickMode;
	}

	@Override
	public TaskType getType() {
		return TaskType.VERIFY;
	}

	@Override
	public String getName() {
		return (quickMode ? "快速校验" : "完整校验") + " dirId=" + dirId;
	}

	@Override
	public TaskProgress getTaskProgress() {
		return taskProgress;
	}

	@Override
	public long getBegin() {
		return begin;
	}

	public boolean isPreCheckPassed() {
		return preCheckPassed;
	}

	public float getMissingRate() {
		return missingRate;
	}

	@Override
	public ProcessResult execute() {
		this.begin = System.currentTimeMillis();
		try {
			RomDir dir = romDirRepo.load(dirId);
			if (dir == null) {
				return new ProcessResult(400, "目录不存在: " + dirId);
			}

			List<RomFile> romFiles = romFileRepo.find(q ->
					q.where(RomFileRepository.dirId.eq(dirId)));
			totalRoms = romFiles.size();

			if (totalRoms == 0) {
				return new ProcessResult(200, "目录无ROM记录");
			}

			File rootDir = new File(dir.getRootpath());

			// 预检阶段
			if (!skipPreCheck) {
				taskProgress = new TaskProgress("预检中...", 0, totalRoms);
				int preCheckMissing = 0;
				for (RomFile rom : romFiles) {
					File file = new File(rootDir, rom.getFilepath());
					if (!file.exists()) {
						preCheckMissing++;
					}
				}
				missingRate = (float) preCheckMissing / totalRoms;

				if (missingRate > 0.7f) {
					preCheckPassed = false;
					String msg = String.format("预检发现 %.0f%% 的文件不存在（%d/%d），可能插错卡或目录不对",
							missingRate * 100, preCheckMissing, totalRoms);
					taskProgress = new TaskProgress(msg, preCheckMissing, totalRoms);
					return new ProcessResult(300, msg);
				}
			}
			preCheckPassed = true;

			// 正式校验
			if (quickMode) {
				doQuickVerify(romFiles, rootDir);
			} else {
				doFullVerify(romFiles, rootDir);
			}

			String msg = String.format("%s完成: 共%d个ROM, 正常%d, 缺失%d, 损坏%d",
					quickMode ? "快速校验" : "完整校验",
					totalRoms, okCount, missingCount, corruptedCount);
			if (noChecksumCount > 0) {
				msg += String.format(", 无校验值%d", noChecksumCount);
			}
			taskProgress = new TaskProgress(msg, okCount + missingCount + corruptedCount, totalRoms);

			int code = 200;
			if (missingCount > 0 || corruptedCount > 0) {
				code = 201;
			}
			ProcessResult result = new ProcessResult(code, msg);
			if (!details.isEmpty()) {
				result.setDetails(details);
			}
			return result;
		} catch (Exception e) {
			log.error("校验任务异常", e);
			return new ProcessResult(500, "校验异常: " + e.getMessage());
		}
	}

	/**
	 * 快速校验模式：
	 * - ZIP文件：从ZIP头读取CRC与记录的CRC比对（零解压开销）
	 * - 非ZIP文件：仅检查文件存在性（不计算hash）
	 */
	private void doQuickVerify(List<RomFile> romFiles, File rootDir) {
		QRomFile t = QRomFile.romFile;

		for (int i = 0; i < romFiles.size(); i++) {
			RomFile rom = romFiles.get(i);
			taskProgress = new TaskProgress("快速校验: " + rom.getName(), okCount + missingCount + corruptedCount, totalRoms);

			File file = new File(rootDir, rom.getFilepath());
			FileStatus newStatus;

			if (!file.exists()) {
				newStatus = FileStatus.MISSING;
				missingCount++;
				details.add("[缺失] " + rom.getFilepath());
			} else if (isZipWrapped(rom) && rom.getCrc() != null && !rom.getCrc().isEmpty()) {
				// ZIP文件且有CRC记录：从ZIP头读取CRC比对
				String actualCrc = readZipEntryCrc(file, rom.getRomName());
				if (actualCrc != null && !actualCrc.equalsIgnoreCase(rom.getCrc())) {
					newStatus = FileStatus.CORRUPTED;
					corruptedCount++;
					details.add("[损坏-CRC] " + rom.getFilepath() + " 期望=" + rom.getCrc() + " 实际=" + actualCrc);
				} else if (actualCrc == null) {
					// 无法读取CRC，视为OK（不误判）
					newStatus = FileStatus.OK;
					okCount++;
				} else {
					newStatus = FileStatus.OK;
					okCount++;
				}
			} else if (!isZipWrapped(rom) && rom.getCrc() != null && !rom.getCrc().isEmpty()) {
				// 非ZIP文件有CRC记录：计算文件CRC比对
				String actualCrc = computeFileCrc(file);
				if (actualCrc != null && !actualCrc.equalsIgnoreCase(rom.getCrc())) {
					newStatus = FileStatus.CORRUPTED;
					corruptedCount++;
					details.add("[损坏-CRC] " + rom.getFilepath() + " 期望=" + rom.getCrc() + " 实际=" + actualCrc);
				} else {
					newStatus = FileStatus.OK;
					okCount++;
				}
			} else {
				// 无CRC记录，文件存在即视为OK
				newStatus = FileStatus.OK;
				okCount++;
				noChecksumCount++;
			}

			// 仅在状态变化时更新数据库
			if (rom.getFileStatus() != newStatus) {
				romFileRepo.getFactory().update(t)
						.set(t.fileStatus, newStatus)
						.where(t.id.eq(rom.getId()))
						.execute();
			}
		}
	}

	/**
	 * 完整校验模式：
	 * - 有MD5记录：解压（如需）并计算MD5比对
	 * - 无MD5但有CRC：用CRC校验
	 * - 都没有：文件存在即OK
	 */
	private void doFullVerify(List<RomFile> romFiles, File rootDir) {
		QRomFile t = QRomFile.romFile;

		for (int i = 0; i < romFiles.size(); i++) {
			RomFile rom = romFiles.get(i);
			taskProgress = new TaskProgress("完整校验: " + rom.getName(), okCount + missingCount + corruptedCount, totalRoms);

			File file = new File(rootDir, rom.getFilepath());
			FileStatus newStatus;

			if (!file.exists()) {
				newStatus = FileStatus.MISSING;
				missingCount++;
				details.add("[缺失] " + rom.getFilepath());
			} else if (rom.getMd5() != null && !rom.getMd5().isEmpty()) {
				// 有MD5记录，校验MD5
				String actualMd5 = computeRomMd5(file, rom);
				if (actualMd5 != null && !actualMd5.equalsIgnoreCase(rom.getMd5())) {
					newStatus = FileStatus.CORRUPTED;
					corruptedCount++;
					details.add("[损坏-MD5] " + rom.getFilepath() + " 期望=" + rom.getMd5() + " 实际=" + actualMd5);
				} else {
					newStatus = FileStatus.OK;
					okCount++;
				}
			} else if (rom.getCrc() != null && !rom.getCrc().isEmpty()) {
				// 无MD5但有CRC，用CRC校验
				String actualCrc;
				if (isZipWrapped(rom)) {
					actualCrc = readZipEntryCrc(file, rom.getRomName());
				} else {
					actualCrc = computeFileCrc(file);
				}
				if (actualCrc != null && !actualCrc.equalsIgnoreCase(rom.getCrc())) {
					newStatus = FileStatus.CORRUPTED;
					corruptedCount++;
					details.add("[损坏-CRC] " + rom.getFilepath() + " 期望=" + rom.getCrc() + " 实际=" + actualCrc);
				} else {
					newStatus = FileStatus.OK;
					okCount++;
				}
			} else {
				// 无校验值，文件存在即OK
				newStatus = FileStatus.OK;
				okCount++;
				noChecksumCount++;
			}

			// 仅在状态变化时更新数据库
			if (rom.getFileStatus() != newStatus) {
				romFileRepo.getFactory().update(t)
						.set(t.fileStatus, newStatus)
						.where(t.id.eq(rom.getId()))
						.execute();
			}
		}
	}

	/**
	 * 判断ROM是否为ZIP包装
	 */
	private boolean isZipWrapped(RomFile rom) {
		return rom.getWrapType() == WrapType.ZIPPED_ROM || rom.getWrapType() == WrapType.ZIPPED_DIRECTORY;
	}

	/**
	 * 从ZIP文件头直接读取指定条目的CRC（不解压，开销极低）
	 */
	private String readZipEntryCrc(File zipFile, String innerFileName) {
		try (ZipFile zf = new ZipFile(zipFile)) {
			ZipEntry entry = zf.getEntry(innerFileName);
			if (entry == null) {
				// 找不到指定文件名，取最大的文件
				entry = zf.stream()
						.filter(e -> !e.isDirectory())
						.max((a, b) -> Long.compare(a.getSize(), b.getSize()))
						.orElse(null);
			}
			if (entry == null) {
				return null;
			}
			long crc = entry.getCrc();
			if (crc == -1) {
				return null;
			}
			return Long.toHexString(crc);
		} catch (IOException e) {
			log.warn("读取ZIP CRC失败: {}", zipFile.getAbsolutePath(), e);
			return null;
		}
	}

	/**
	 * 计算文件CRC32（用于非ZIP文件的快速校验）
	 */
	private String computeFileCrc(File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			CRC32 crc32 = new CRC32();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = fis.read(buffer)) != -1) {
				crc32.update(buffer, 0, read);
			}
			return Long.toHexString(crc32.getValue());
		} catch (IOException e) {
			log.warn("计算CRC失败: {}", file.getAbsolutePath(), e);
			return null;
		}
	}

	/**
	 * 计算ROM文件的MD5。
	 * 如果是ZIP包装，读取ZIP内主文件的MD5。
	 */
	private String computeRomMd5(File file, RomFile romFile) {
		if (isZipWrapped(romFile)) {
			return computeZipInnerMd5(file, romFile.getRomName());
		} else {
			return computeFileMd5(file);
		}
	}

	private String computeZipInnerMd5(File zipFile, String innerFileName) {
		try (ZipFile zf = new ZipFile(zipFile)) {
			ZipEntry entry = zf.getEntry(innerFileName);
			if (entry == null) {
				entry = zf.stream()
						.filter(e -> !e.isDirectory())
						.max((a, b) -> Long.compare(a.getSize(), b.getSize()))
						.orElse(null);
			}
			if (entry == null) {
				return null;
			}
			try (InputStream is = zf.getInputStream(entry)) {
				return computeStreamMd5(is);
			}
		} catch (IOException e) {
			log.warn("读取ZIP计算MD5失败: {}", zipFile.getAbsolutePath(), e);
			return null;
		}
	}

	private String computeFileMd5(File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			return computeStreamMd5(fis);
		} catch (IOException e) {
			log.warn("计算MD5失败: {}", file.getAbsolutePath(), e);
			return null;
		}
	}

	private String computeStreamMd5(InputStream is) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] buffer = new byte[8192];
			int read;
			while ((read = is.read(buffer)) != -1) {
				md.update(buffer, 0, read);
			}
			byte[] digest = md.digest();
			StringBuilder sb = new StringBuilder(32);
			for (byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException | IOException e) {
			log.warn("计算MD5失败", e);
			return null;
		}
	}
}
