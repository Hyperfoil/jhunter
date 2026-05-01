package io.hyperfoil.tools.jhunter;

/**
 * Configuration for the {@link Analysis} change point detection algorithm.
 *
 * @param windowLen sliding window size for the split phase (must be >= 3)
 * @param maxPvalue significance threshold; change points with p-value above this are discarded
 * @param minMagnitude minimum relative change magnitude to report (0.1 = 10%)
 */
public record AnalysisOptions(
        int windowLen,
        double maxPvalue,
        double minMagnitude
) {
    public static final int DEFAULT_WINDOW_LEN = 50;
    public static final double DEFAULT_MAX_PVALUE = 0.001;
    public static final double DEFAULT_MIN_MAGNITUDE = 0.0;

    public AnalysisOptions {
        if (windowLen < 3) {
            throw new IllegalArgumentException("windowLen must be >= 3, got " + windowLen);
        }
        if (maxPvalue <= 0.0 || maxPvalue > 1.0) {
            throw new IllegalArgumentException("maxPvalue must be in (0.0, 1.0], got " + maxPvalue);
        }
        if (minMagnitude < 0.0) {
            throw new IllegalArgumentException("minMagnitude must be >= 0.0, got " + minMagnitude);
        }
    }

    public AnalysisOptions() {
        this(DEFAULT_WINDOW_LEN, DEFAULT_MAX_PVALUE, DEFAULT_MIN_MAGNITUDE);
    }

    public AnalysisOptions(double maxPvalue) {
        this(DEFAULT_WINDOW_LEN, maxPvalue, DEFAULT_MIN_MAGNITUDE);
    }
}
