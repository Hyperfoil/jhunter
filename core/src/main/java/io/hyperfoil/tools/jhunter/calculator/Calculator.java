package io.hyperfoil.tools.jhunter.calculator;

import io.hyperfoil.tools.jhunter.ChangePoint;

public abstract class Calculator {

    protected final double[] series;

    protected Calculator(double[] series) {
        this.series = series;
    }

    public abstract ChangePoint getCandidateChangePoint(int start, int end);

    public ChangePoint getNextCandidate(int[][] intervals) {
        ChangePoint best = null;
        for (int[] interval : intervals) {
            if (interval[1] - interval[0] <= 1) continue;
            ChangePoint candidate = getCandidateChangePoint(interval[0], interval[1]);
            if (best == null || candidate.qhat() > best.qhat()) {
                best = candidate;
            }
        }
        return best;
    }
}
