package io.github.xuse.romking.tasks;

public interface Task {
	TaskType getType();

	String getName();

	long getBegin();

	/**
	 * Returns structured progress data.
	 */
	default TaskProgress getTaskProgress() {
		return null;
	}

	/**
	 * Backward-compatible string representation.
	 * Default delegates to getTaskProgress().toString().
	 */
	default String getProgress() {
		TaskProgress tp = getTaskProgress();
		return tp != null ? tp.toString() : "";
	}

	ProcessResult execute();
}
