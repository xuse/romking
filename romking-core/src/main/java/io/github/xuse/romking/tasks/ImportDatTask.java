package io.github.xuse.romking.tasks;

import java.io.File;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.nointro.DatImportService;
import lombok.extern.slf4j.Slf4j;

/**
 * DAT文件导入任务。
 * 支持导入单个DAT文件或整个目录，实时更新进度。
 */
@Slf4j
public class ImportDatTask implements Task {

	private final File path;
	private final Platform platform;
	private final DatImportService importService;
	private final boolean isParentClone;
	/** 要导入的平台集合，null表示全部 */
	private final Set<Platform> platforms;

	private long begin;
	private volatile TaskProgress taskProgress = new TaskProgress("等待开始", 0, 0);

	/** Pattern to parse progress callback: [processed/total] fileName (已导入N条) */
	private static final Pattern PROGRESS_PATTERN = Pattern.compile("\\[(\\d+)/(\\d+)\\]\\s+(.+)");

	/**
	 * @param path DAT文件或包含DAT文件的目录
	 * @param platform 平台（null则自动推断）
	 * @param isParentClone 是否为P/C格式（补充parent/clone关系）
	 * @param platforms 要导入的平台集合（null表示全部）
	 */
	public ImportDatTask(File path, Platform platform, boolean isParentClone,
			Set<Platform> platforms, DatImportService importService) {
		this.path = path;
		this.platform = platform;
		this.isParentClone = isParentClone;
		this.platforms = platforms;
		this.importService = importService;
	}

	@Override
	public TaskType getType() {
		return TaskType.IMPORT_DAT;
	}

	@Override
	public String getName() {
		return (isParentClone ? "导入P/C " : "导入DAT ") + path.getName();
	}

	@Override
	public TaskProgress getTaskProgress() {
		return taskProgress;
	}

	@Override
	public long getBegin() {
		return begin;
	}

	@Override
	public ProcessResult execute() {
		this.begin = System.currentTimeMillis();
		try {
			int count;
			if (path.isDirectory()) {
				taskProgress = new TaskProgress("正在扫描目录: " + path.getName(), 0, 0);
				count = importService.importDirectory(path, platforms, this::updateProgress);
			} else if (isParentClone) {
				taskProgress = new TaskProgress("正在导入P/C: " + path.getName(), 0, 1);
				count = importService.importParentClone(path, platform);
				taskProgress = new TaskProgress("正在导入P/C: " + path.getName(), 1, 1);
			} else {
				taskProgress = new TaskProgress("正在导入: " + path.getName(), 0, 1);
				count = importService.importDat(path, platform);
				taskProgress = new TaskProgress("正在导入: " + path.getName(), 1, 1);
			}

			long elapsed = System.currentTimeMillis() - begin;
			String msg = String.format("导入完成: %d条记录, 耗时%.1f秒", count, elapsed / 1000.0);
			taskProgress = new TaskProgress(msg, 1, 1);
			return new ProcessResult(200, msg);
		} catch (Exception e) {
			log.error("DAT导入异常", e);
			String msg = "导入异常: " + e.getMessage();
			taskProgress = new TaskProgress(msg, 0, 0);
			return new ProcessResult(500, msg);
		}
	}

	/**
	 * 进度回调，解析 DatImportService 传来的格式化字符串并转为 TaskProgress。
	 * 格式: [processed/total] fileName (已导入N条)
	 */
	private void updateProgress(String msg) {
		Matcher m = PROGRESS_PATTERN.matcher(msg);
		if (m.find()) {
			int processed = Integer.parseInt(m.group(1));
			int total = Integer.parseInt(m.group(2));
			String step = m.group(3);
			this.taskProgress = new TaskProgress(step, processed, total);
		} else {
			// Fallback: use the raw message as step description
			this.taskProgress = new TaskProgress(msg, 0, 0);
		}
	}
}
