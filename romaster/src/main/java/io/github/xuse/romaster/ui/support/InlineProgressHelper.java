package io.github.xuse.romaster.ui.support;

import java.util.function.Consumer;

import com.vaadin.flow.component.UI;

import io.github.xuse.romking.service.GlobalTaskService;
import io.github.xuse.romking.tasks.ProcessResult;
import io.github.xuse.romking.tasks.Task;
import io.github.xuse.romking.tasks.TaskProgress;
import io.github.xuse.romking.tasks.TaskProgressListener;

public class InlineProgressHelper implements TaskProgressListener {

    private final Task subscribedTask;
    private final UI ui;
    private final Consumer<TaskProgress> progressCallback;
    private final Consumer<ProcessResult> completionCallback;
    private final GlobalTaskService taskService;

    public InlineProgressHelper(Task task, UI ui,
            Consumer<TaskProgress> progressCallback,
            Consumer<ProcessResult> completionCallback,
            GlobalTaskService taskService) {
        this.subscribedTask = task;
        this.ui = ui;
        this.progressCallback = progressCallback;
        this.completionCallback = completionCallback;
        this.taskService = taskService;
        taskService.addListener(this);
    }

    @Override
    public void onProgressChanged(Task task, TaskProgress progress) {
        if (task != subscribedTask) return;
        ui.access(() -> progressCallback.accept(progress));
    }

    @Override
    public void onTaskCompleted(Task task, ProcessResult result) {
        if (task != subscribedTask) return;
        ui.access(() -> completionCallback.accept(result));
        unsubscribe();
    }

    public void unsubscribe() {
        taskService.removeListener(this);
    }
}
