package io.hyperfoil.tools.jhunter.significance;

import io.hyperfoil.tools.jhunter.ChangePoint;
import io.hyperfoil.tools.jhunter.math.TDistribution;

public class TTestSignificanceTester extends SignificanceTester {

    public TTestSignificanceTester(double maxPvalue) {
        super(maxPvalue);
    }

    @Override
    public ChangePoint test(ChangePoint candidate, double[] series, int[][] intervals) {
        int[] leftRight = findSegments(candidate.index(), intervals);
        int leftStart = leftRight[0];
        int leftEnd = leftRight[1];
        int rightStart = leftRight[2];
        int rightEnd = leftRight[3];

        return compare(candidate, series, leftStart, leftEnd, rightStart, rightEnd);
    }

    ChangePoint compare(ChangePoint candidate, double[] series,
                         int leftStart, int leftEnd, int rightStart, int rightEnd) {
        int nL = leftEnd - leftStart;
        int nR = rightEnd - rightStart;

        double meanL = mean(series, leftStart, leftEnd);
        double meanR = mean(series, rightStart, rightEnd);
        double stdL = std(series, leftStart, leftEnd, meanL);
        double stdR = std(series, rightStart, rightEnd, meanR);

        if (nL < 2 || nR < 2) {
            return candidate.withStats(1.0, meanL, meanR, stdL, stdR);
        }

        double t = TDistribution.welchT(meanL, stdL, nL, meanR, stdR, nR);
        double df = TDistribution.welchDf(stdL, nL, stdR, nR);
        double pvalue = TDistribution.twoTailedPvalue(t, df);

        return candidate.withStats(pvalue, meanL, meanR, stdL, stdR);
    }

    private int[] findSegments(int index, int[][] intervals) {
        for (int i = 0; i < intervals.length; i++) {
            int start = intervals[i][0];
            int end = intervals[i][1];
            if (index > start && index < end) {
                return new int[]{start, index, index, end};
            }
            if (i < intervals.length - 1 && index == end) {
                return new int[]{start, end, intervals[i + 1][0], intervals[i + 1][1]};
            }
        }
        return new int[]{intervals[0][0], index, index, intervals[intervals.length - 1][1]};
    }

    static double mean(double[] values, int start, int end) {
        int n = end - start;
        if (n == 0) return 0;
        double sum = 0;
        for (int i = start; i < end; i++) sum += values[i];
        return sum / n;
    }

    static double std(double[] values, int start, int end, double mean) {
        int n = end - start;
        if (n < 2) return 0;
        double sum = 0;
        for (int i = start; i < end; i++) {
            double d = values[i] - mean;
            sum += d * d;
        }
        return Math.sqrt(sum / (n - 1));
    }
}
