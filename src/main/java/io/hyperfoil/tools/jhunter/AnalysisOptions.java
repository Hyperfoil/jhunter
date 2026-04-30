package io.hyperfoil.tools.jhunter;

public record AnalysisOptions(
        int windowLen,
        double maxPvalue,
        double minMagnitude
) {
    public static final int DEFAULT_WINDOW_LEN = 50;
    public static final double DEFAULT_MAX_PVALUE = 0.001;
    public static final double DEFAULT_MIN_MAGNITUDE = 0.0;

    public AnalysisOptions() {
        this(DEFAULT_WINDOW_LEN, DEFAULT_MAX_PVALUE, DEFAULT_MIN_MAGNITUDE);
    }

    public AnalysisOptions(double maxPvalue) {
        this(DEFAULT_WINDOW_LEN, maxPvalue, DEFAULT_MIN_MAGNITUDE);
    }
}
