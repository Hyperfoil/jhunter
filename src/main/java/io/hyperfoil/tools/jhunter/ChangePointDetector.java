package io.hyperfoil.tools.jhunter;

import io.hyperfoil.tools.jhunter.calculator.Calculator;
import io.hyperfoil.tools.jhunter.significance.SignificanceTester;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChangePointDetector {

    private final Calculator calculator;
    private final SignificanceTester tester;

    public ChangePointDetector(Calculator calculator, SignificanceTester tester) {
        this.calculator = calculator;
        this.tester = tester;
    }

    public List<ChangePoint> detect(double[] series) {
        return detect(series, 0, series.length);
    }

    public List<ChangePoint> detect(double[] series, int start, int end) {
        List<ChangePoint> changePoints = new ArrayList<>();

        while (true) {
            int[][] intervals = SignificanceTester.getIntervals(changePoints, end);
            // Adjust intervals to start from 'start'
            if (intervals.length > 0 && intervals[0][0] < start) {
                intervals[0][0] = start;
            }

            ChangePoint candidate = calculator.getNextCandidate(intervals);
            if (candidate == null) break;

            ChangePoint tested = tester.test(candidate, series, intervals);
            if (tester.isSignificant(tested)) {
                changePoints.add(tested);
                changePoints.sort(Comparator.comparingInt(ChangePoint::index));
            } else {
                break;
            }
        }

        return changePoints;
    }
}
