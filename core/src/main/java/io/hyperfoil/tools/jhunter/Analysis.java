package io.hyperfoil.tools.jhunter;

import io.hyperfoil.tools.jhunter.calculator.PairDistanceCalculator;
import io.hyperfoil.tools.jhunter.significance.PermutationSignificanceTester;
import io.hyperfoil.tools.jhunter.significance.SignificanceTester;
import io.hyperfoil.tools.jhunter.significance.TTestSignificanceTester;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Entry point for change point detection in time series data.
 * Provides Hunter's two-step (split/merge) algorithm and the original Matteson-James algorithm.
 */
public final class Analysis {

    private Analysis() {}

    /**
     * Detects change points using Hunter's two-step algorithm (recommended).
     * Slides a window across the series to find candidates, then filters with strict significance testing.
     *
     * @param series the time series data (must have at least 3 elements)
     * @param options detection parameters (window size, p-value threshold, minimum magnitude)
     * @return change points sorted by index, or empty list if none found
     */
    public static List<ChangePoint> computeChangePoints(double[] series, AnalysisOptions options) {
        if (series.length < 3) return List.of();

        List<ChangePoint> weakPoints = split(series, options.windowLen(), options.maxPvalue());
        return merge(weakPoints, series, options.maxPvalue(), options.minMagnitude());
    }

    /**
     * Detects change points using the original Matteson-James E-Divisive algorithm with permutation testing.
     * More theoretically sound but O(n² x permutations) per change point.
     *
     * @param series the time series data (must have at least 3 elements)
     * @param maxPvalue significance threshold for permutation testing
     * @param seed random seed for reproducible permutations
     * @return change points sorted by index, or empty list if none found
     */
    public static List<ChangePoint> computeChangePointsOriginal(double[] series, double maxPvalue, long seed) {
        if (series.length < 3) return List.of();

        PairDistanceCalculator calculator = new PairDistanceCalculator(series);
        PermutationSignificanceTester tester = new PermutationSignificanceTester(maxPvalue, 100, seed);
        ChangePointDetector detector = new ChangePointDetector(calculator, tester);
        return detector.detect(series);
    }

    static List<ChangePoint> split(double[] series, int windowLen, double maxPvalue) {
        double relaxedPvalue = maxPvalue > 0.05 ? maxPvalue * 2 : maxPvalue * 10;
        int step = Math.max(1, windowLen / 2);
        List<ChangePoint> weakPoints = new ArrayList<>();

        for (int start = 0; start < series.length - 1; start += step) {
            int end = Math.min(start + windowLen, series.length);
            if (end - start < 3) continue;

            double[] window = new double[end - start];
            System.arraycopy(series, start, window, 0, window.length);

            PairDistanceCalculator calculator = new PairDistanceCalculator(window);
            TTestSignificanceTester tester = new TTestSignificanceTester(relaxedPvalue);
            ChangePointDetector detector = new ChangePointDetector(calculator, tester);

            List<ChangePoint> windowPoints = detector.detect(window);
            for (ChangePoint cp : windowPoints) {
                int globalIndex = cp.index() + start;
                ChangePoint global = new ChangePoint(globalIndex, cp.qhat(), cp.pvalue(),
                        cp.meanBefore(), cp.meanAfter(), cp.stdBefore(), cp.stdAfter());

                boolean duplicate = weakPoints.stream()
                        .anyMatch(existing -> existing.index() == globalIndex);
                if (!duplicate) {
                    weakPoints.add(global);
                }
            }
        }

        weakPoints.sort(Comparator.comparingInt(ChangePoint::index));
        return recomputeStats(weakPoints, series);
    }

    static List<ChangePoint> merge(List<ChangePoint> weakPoints, double[] series,
                                   double maxPvalue, double minMagnitude) {
        if (weakPoints.isEmpty()) return List.of();

        List<ChangePoint> points = new ArrayList<>(weakPoints);

        while (!points.isEmpty()) {
            // Find the point that fails thresholds and has the smallest magnitude
            ChangePoint weakest = null;
            int weakestIdx = -1;
            for (int i = 0; i < points.size(); i++) {
                ChangePoint cp = points.get(i);
                if (cp.pvalue() > maxPvalue || cp.magnitude() < minMagnitude) {
                    if (weakest == null || cp.magnitude() < weakest.magnitude()) {
                        weakest = cp;
                        weakestIdx = i;
                    }
                }
            }

            if (weakest == null) break;

            points.remove(weakestIdx);

            // Recompute stats for adjacent points after removal
            points = recomputeStats(points, series);
        }

        return points;
    }

    private static List<ChangePoint> recomputeStats(List<ChangePoint> points, double[] series) {
        if (points.isEmpty()) return points;

        TTestSignificanceTester tester = new TTestSignificanceTester(1.0);
        int[][] intervals = SignificanceTester.getIntervals(points, series.length);

        List<ChangePoint> recomputed = new ArrayList<>();
        for (ChangePoint cp : points) {
            ChangePoint updated = tester.test(cp, series, intervals);
            recomputed.add(updated);
        }
        return recomputed;
    }
}
