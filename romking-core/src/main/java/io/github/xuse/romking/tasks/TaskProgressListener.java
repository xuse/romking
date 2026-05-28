package io.github.xuse.romking.tasks;

public interface TaskProgressListener {
	void onProgressChanged(Task task, TaskProgress progress);

	void onTaskCompleted(Task task, ProcessResult result);
}
