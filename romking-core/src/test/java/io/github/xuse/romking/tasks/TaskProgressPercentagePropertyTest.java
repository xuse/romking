package io.github.xuse.romking.tasks;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for TaskProgress percentage computation and clamping.
 *
 * <p><b>Validates: Requirements 1.4, 1.5, 1.6</b></p>
 *
 * <p>Property 1: For any processedItems (≥ 0) and totalItems (≥ 0) values used to construct
 * a TaskProgress, the resulting percentage field SHALL equal (processedItems * 100) / totalItems
 * when totalItems > 0, SHALL equal 0 when totalItems == 0, and SHALL always be within the
 * range [0, 100] inclusive.</p>
 */
class TaskProgressPercentagePropertyTest {

    /**
     * Property: percentage is always within [0, 100] inclusive for any non-negative inputs.
     */
    @Property
    void percentageIsAlwaysWithinBounds(
            @ForAll @IntRange(min = 0, max = Integer.MAX_VALUE / 100) int processedItems,
            @ForAll @IntRange(min = 0, max = Integer.MAX_VALUE) int totalItems) {

        TaskProgress progress = new TaskProgress("test step", processedItems, totalItems);

        assertTrue(progress.getPercentage() >= 0,
                "Percentage should be >= 0 but was " + progress.getPercentage());
        assertTrue(progress.getPercentage() <= 100,
                "Percentage should be <= 100 but was " + progress.getPercentage());
    }

    /**
     * Property: when totalItems == 0, percentage SHALL be 0.
     */
    @Property
    void percentageIsZeroWhenTotalItemsIsZero(
            @ForAll @IntRange(min = 0, max = Integer.MAX_VALUE) int processedItems) {

        TaskProgress progress = new TaskProgress("test step", processedItems, 0);

        assertEquals(0, progress.getPercentage(),
                "Percentage should be 0 when totalItems is 0, but was " + progress.getPercentage());
    }

    /**
     * Property: when totalItems > 0, percentage SHALL equal (processedItems * 100) / totalItems,
     * clamped to [0, 100].
     */
    @Property
    void percentageComputedCorrectlyWhenTotalItemsPositive(
            @ForAll @IntRange(min = 0, max = Integer.MAX_VALUE / 100) int processedItems,
            @ForAll @IntRange(min = 1, max = Integer.MAX_VALUE) int totalItems) {

        TaskProgress progress = new TaskProgress("test step", processedItems, totalItems);

        int expectedRaw = (processedItems * 100) / totalItems;
        int expectedClamped = Math.min(100, Math.max(0, expectedRaw));

        assertEquals(expectedClamped, progress.getPercentage(),
                String.format("For processedItems=%d, totalItems=%d: expected %d but got %d",
                        processedItems, totalItems, expectedClamped, progress.getPercentage()));
    }

    /**
     * Property: percentage never exceeds 100 even when processedItems > totalItems.
     */
    @Property
    void percentageClampedAt100WhenProcessedExceedsTotal(
            @ForAll @IntRange(min = 1, max = 10000) int totalItems,
            @ForAll @IntRange(min = 1, max = 100) int multiplier) {

        int processedItems = totalItems * multiplier;
        TaskProgress progress = new TaskProgress("test step", processedItems, totalItems);

        assertTrue(progress.getPercentage() <= 100,
                "Percentage should be <= 100 when processedItems > totalItems, but was " + progress.getPercentage());
    }
}
