package io.hyperfoil.tools.jhunter;

import io.hyperfoil.tools.jhunter.calculator.Calculator;
import io.hyperfoil.tools.jhunter.significance.SignificanceTester;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Iteratively finds change points by selecting the strongest candidate and testing its significance,
 * repeating until no more significant change points remain.
 */
public class ChangePointDetector {

    private final Calculator calculator;
    private final SignificanceTester tester;

    /**
     * @param calculator computes candidate change points (e.g. {@link io.hyperfoil.tools.jhunter.calculator.PairDistanceCalculator})
     * @param tester determines statistical significance of candidates
     */
    public ChangePointDetector(Calculator calculator, SignificanceTester tester) {
        this.calculator = calculator;
        this.tester = tester;
    }

    /** Detects all significant change points in the series. */
    public List<ChangePoint> detect(double[] series) {
        return detect(series, 0, series.length);
    }

    /** Detects all significant change points within the {@code [start, end)} range of the series. */
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
