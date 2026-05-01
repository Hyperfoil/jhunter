package io.hyperfoil.tools.jhunter.significance;

import io.hyperfoil.tools.jhunter.ChangePoint;

import java.util.ArrayList;
import java.util.List;

public abstract class SignificanceTester {

    protected final double maxPvalue;

    protected SignificanceTester(double maxPvalue) {
        this.maxPvalue = maxPvalue;
    }

    public abstract ChangePoint test(ChangePoint candidate, double[] series, int[][] intervals);

    public boolean isSignificant(ChangePoint cp) {
        return !Double.isNaN(cp.pvalue()) && cp.pvalue() <= maxPvalue;
    }

    public static int[][] getIntervals(List<ChangePoint> changePoints, int seriesLength) {
        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(0);
        for (ChangePoint cp : changePoints) {
            boundaries.add(cp.index());
        }
        boundaries.add(seriesLength);

        int[][] intervals = new int[boundaries.size() - 1][2];
        for (int i = 0; i < intervals.length; i++) {
            intervals[i][0] = boundaries.get(i);
            intervals[i][1] = boundaries.get(i + 1);
        }
        return intervals;
    }
}
