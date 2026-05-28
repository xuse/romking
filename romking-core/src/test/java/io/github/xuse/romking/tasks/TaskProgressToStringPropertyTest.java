package io.github.xuse.romking.tasks;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for TaskProgress toString format.
 *
 * **Validates: Requirements 3.2**
 *
 * Property 10: For any TaskProgress instance, toString() SHALL produce a string
 * containing the percentage value, the currentStep text, the processedItems count,
 * and the totalItems count.
 *
 * Expected format: [%d%%] %s (%d/%d) — e.g., [50%] Scanning files (5/10)
 */
class TaskProgressToStringPropertyTest {

    @Property
    void toStringContainsAllFieldsInCorrectFormat(
            @ForAll("currentSteps") String currentStep,
            @ForAll @IntRange(min = 0, max = 10000) int processedItems,
            @ForAll @IntRange(min = 0, max = 10000) int totalItems) {

        TaskProgress progress = new TaskProgress(currentStep, processedItems, totalItems);
        String result = progress.toString();

        int expectedPercentage = progress.getPercentage();

        // Verify the toString output matches the exact format: [%d%%] %s (%d/%d)
        String expected = String.format("[%d%%] %s (%d/%d)",
                expectedPercentage, currentStep, processedItems, totalItems);
        assertEquals(expected, result);
    }

    @Property
    void toStringContainsPercentageValue(
            @ForAll("currentSteps") String currentStep,
            @ForAll @IntRange(min = 0, max = 10000) int processedItems,
            @ForAll @IntRange(min = 0, max = 10000) int totalItems) {

        TaskProgress progress = new TaskProgress(currentStep, processedItems, totalItems);
        String result = progress.toString();

        // The percentage value must appear in the output
        assertTrue(result.contains(progress.getPercentage() + "%"),
                "toString should contain percentage value");
    }

    @Property
    void toStringContainsCurrentStep(
            @ForAll("currentSteps") String currentStep,
            @ForAll @IntRange(min = 0, max = 10000) int processedItems,
            @ForAll @IntRange(min = 0, max = 10000) int totalItems) {

        TaskProgress progress = new TaskProgress(currentStep, processedItems, totalItems);
        String result = progress.toString();

        // The currentStep text must appear in the output
        assertTrue(result.contains(currentStep),
                "toString should contain currentStep: " + result);
    }

    @Property
    void toStringContainsProcessedAndTotalItems(
            @ForAll("currentSteps") String currentStep,
            @ForAll @IntRange(min = 0, max = 10000) int processedItems,
            @ForAll @IntRange(min = 0, max = 10000) int totalItems) {

        TaskProgress progress = new TaskProgress(currentStep, processedItems, totalItems);
        String result = progress.toString();

        // The processedItems/totalItems must appear in the output
        assertTrue(result.contains(processedItems + "/" + totalItems),
                "toString should contain processedItems/totalItems: " + result);
    }

    @Provide
    Arbitrary<String> currentSteps() {
        return Arbitraries.of(
                "Scanning files",
                "正在导入",
                "Exporting ROMs",
                "Verifying checksums",
                "归档中",
                "Processing",
                "Step 1 of 3",
                "等待开始"
        );
    }
}
