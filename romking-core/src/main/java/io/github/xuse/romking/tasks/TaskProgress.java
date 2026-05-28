package io.github.xuse.romking.tasks;

import lombok.Getter;

/**
 * Immutable value object representing structured task progress state.
 * Each progress update creates a new instance, avoiding concurrency issues
 * when listeners read progress from different threads.
 */
@Getter
public class TaskProgress {
    private final String currentStep;
    private final int processedItems;
    private final int totalItems;
    private final int percentage;

    public TaskProgress(String currentStep, int processedItems, int totalItems) {
        this.currentStep = currentStep;
        this.processedItems = processedItems;
        this.totalItems = totalItems;
        this.percentage = computePercentage(processedItems, totalItems);
    }

    private static int computePercentage(int processed, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min(100, Math.max(0, (processed * 100) / total));
    }

    @Override
    public String toString() {
        return String.format("[%d%%] %s (%d/%d)", percentage, currentStep, processedItems, totalItems);
    }
}
