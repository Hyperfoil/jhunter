package io.hyperfoil.tools.jhunter;

/**
 * A detected change point in a time series.
 *
 * @param index position in the series where the distribution changes
 * @param qhat Q-hat divergence statistic (higher = more distinct segments)
 * @param pvalue statistical significance (lower = more significant)
 * @param meanBefore mean of the segment before the change point
 * @param meanAfter mean of the segment after the change point
 * @param stdBefore standard deviation of the segment before the change point
 * @param stdAfter standard deviation of the segment after the change point
 */
public record ChangePoint(
        int index,
        double qhat,
        double pvalue,
        double meanBefore,
        double meanAfter,
        double stdBefore,
        double stdAfter
) {
    /** Relative change as {@code (meanAfter / meanBefore) - 1}. Returns NaN if meanBefore is zero. */
    public double forwardChangePercent() {
        if (meanBefore == 0.0) return Double.NaN;
        return (meanAfter / meanBefore) - 1.0;
    }

    /** Reverse relative change as {@code (meanBefore / meanAfter) - 1}. Returns NaN if meanAfter is zero. */
    public double backwardChangePercent() {
        if (meanAfter == 0.0) return Double.NaN;
        return (meanBefore / meanAfter) - 1.0;
    }

    /** Symmetric measure of change size: {@code max(abs(forward), abs(backward))}. */
    public double magnitude() {
        double fwd = Math.abs(forwardChangePercent());
        double bwd = Math.abs(backwardChangePercent());
        if (Double.isNaN(fwd)) return bwd;
        if (Double.isNaN(bwd)) return fwd;
        return Math.max(fwd, bwd);
    }

    public static ChangePoint candidate(int index, double qhat) {
        return new ChangePoint(index, qhat, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    public ChangePoint withStats(double pvalue, double meanBefore, double meanAfter, double stdBefore, double stdAfter) {
        return new ChangePoint(index, qhat, pvalue, meanBefore, meanAfter, stdBefore, stdAfter);
    }
}
