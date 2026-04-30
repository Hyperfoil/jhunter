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

        double[] left = slice(series, leftStart, leftEnd);
        double[] right = slice(series, rightStart, rightEnd);

        return compare(candidate, left, right);
    }

    ChangePoint compare(ChangePoint candidate, double[] left, double[] right) {
        if (left.length < 2 || right.length < 2) {
            return candidate.withStats(1.0, mean(left), mean(right), std(left), std(right));
        }

        double meanL = mean(left);
        double meanR = mean(right);
        double stdL = std(left);
        double stdR = std(right);

        double t = TDistribution.welchT(meanL, stdL, left.length, meanR, stdR, right.length);
        double df = TDistribution.welchDf(stdL, left.length, stdR, right.length);
        double pvalue = TDistribution.twoTailedPvalue(t, df);

        return candidate.withStats(pvalue, meanL, meanR, stdL, stdR);
    }

    private int[] findSegments(int index, int[][] intervals) {
        for (int i = 0; i < intervals.length; i++) {
            int start = intervals[i][0];
            int end = intervals[i][1];
            if (index > start && index < end) {
                // Split: candidate splits an existing interval
                return new int[]{start, index, index, end};
            }
            if (i < intervals.length - 1 && index == end) {
                // Merge: candidate is at a boundary between two intervals
                return new int[]{start, end, intervals[i + 1][0], intervals[i + 1][1]};
            }
        }
        // Fallback: use the full series range from first to last interval
        return new int[]{intervals[0][0], index, index, intervals[intervals.length - 1][1]};
    }

    private static double[] slice(double[] array, int start, int end) {
        double[] result = new double[end - start];
        System.arraycopy(array, start, result, 0, result.length);
        return result;
    }

    static double mean(double[] values) {
        if (values.length == 0) return 0;
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    static double std(double[] values) {
        if (values.length < 2) return 0;
        double m = mean(values);
        double sum = 0;
        for (double v : values) sum += (v - m) * (v - m);
        return Math.sqrt(sum / (values.length - 1));
    }
}
