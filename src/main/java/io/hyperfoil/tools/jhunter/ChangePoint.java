package io.hyperfoil.tools.jhunter;

public record ChangePoint(
        int index,
        double qhat,
        double pvalue,
        double meanBefore,
        double meanAfter,
        double stdBefore,
        double stdAfter
) {
    public double forwardChangePercent() {
        if (meanBefore == 0.0) return Double.NaN;
        return (meanAfter / meanBefore) - 1.0;
    }

    public double backwardChangePercent() {
        if (meanAfter == 0.0) return Double.NaN;
        return (meanBefore / meanAfter) - 1.0;
    }

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
