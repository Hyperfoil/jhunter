package io.hyperfoil.tools.jhunter;

import io.hyperfoil.tools.jhunter.calculator.PairDistanceCalculator;
import io.hyperfoil.tools.jhunter.significance.PermutationSignificanceTester;
import io.hyperfoil.tools.jhunter.significance.TTestSignificanceTester;
import io.hyperfoil.tools.jhunter.significance.SignificanceTester;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-validation tests ported from Apache Otava's Python test suite.
 * These use the exact same test data and expected results as Otava's
 * change_point_divisive_test.py and analysis_test.py.
 */
class OtavaCrossValidationTest {

    // From change_point_divisive_test.py
    static final double[] SEQUENCE = {
            0.3, 2.4, 1.5, -0.9, -0.5,
            99.7, 98.3, 99.1,
            149.0, 149.7, 149.5, 149.1, 148.8, 150.0
    };
    static final int[] EXPECTED_CHANGE_POINTS = {5, 8};

    // From analysis_test.py
    static final double[] SINGLE_SERIES = {
            1.02, 0.95, 0.99, 1.00, 1.12,
            1.00, 1.01, 0.98, 1.01, 0.96,
            0.50, 0.51, 0.48, 0.48, 0.55,
            0.50, 0.49, 0.51, 0.50, 0.49
    };

    @Test
    void calculatorFindsCorrectCandidate() {
        // Matches test_calculator_candidate
        PairDistanceCalculator calc = new PairDistanceCalculator(SEQUENCE);
        ChangePoint candidate = calc.getCandidateChangePoint(0, SEQUENCE.length);

        // The first candidate should be at index 5 (the biggest jump)
        // or 8 — either is valid as the maximum Q position
        assertTrue(candidate.index() == 5 || candidate.index() == 8,
                "candidate should be at index 5 or 8, was " + candidate.index());
        assertTrue(candidate.qhat() > 0, "qhat should be positive");
    }

    @Test
    void ttestDetectsChangePointsInSequence() {
        // Matches test_ttest
        PairDistanceCalculator calc = new PairDistanceCalculator(SEQUENCE);
        TTestSignificanceTester tester = new TTestSignificanceTester(0.01);
        ChangePointDetector detector = new ChangePointDetector(calc, tester);

        List<ChangePoint> cps = detector.detect(SEQUENCE);
        int[] indices = cps.stream().mapToInt(ChangePoint::index).sorted().toArray();

        assertArrayEquals(EXPECTED_CHANGE_POINTS, indices,
                "t-test should detect change points at [5, 8], got " + java.util.Arrays.toString(indices));
    }

    @Test
    void permutationTestDetectsChangePointsInSequence() {
        // Matches test_permutation_test — uses more permutations since
        // Java's Random produces different shuffles than numpy's default_rng
        PairDistanceCalculator calc = new PairDistanceCalculator(SEQUENCE);
        PermutationSignificanceTester tester = new PermutationSignificanceTester(0.05, 200, 42);
        ChangePointDetector detector = new ChangePointDetector(calc, tester);

        List<ChangePoint> cps = detector.detect(SEQUENCE);
        int[] indices = cps.stream().mapToInt(ChangePoint::index).sorted().toArray();

        assertArrayEquals(EXPECTED_CHANGE_POINTS, indices,
                "permutation test should detect change points at [5, 8], got " + java.util.Arrays.toString(indices));
    }

    @Test
    void singleSeriesChangePointAtIndex10() {
        // Matches test_single_series
        List<ChangePoint> cps = Analysis.computeChangePoints(
                SINGLE_SERIES, new AnalysisOptions(10, 0.0001, 0.0));

        int[] indices = cps.stream().mapToInt(ChangePoint::index).toArray();
        assertArrayEquals(new int[]{10}, indices,
                "should detect change point at [10], got " + java.util.Arrays.toString(indices));
    }

    @Test
    void singleSeriesOriginalAlgorithm() {
        // Matches test_single_series_original — relaxed pvalue and more permutations
        // since Java's Random differs from numpy's default_rng
        List<ChangePoint> cps = Analysis.computeChangePointsOriginal(SINGLE_SERIES, 0.01, 42);

        int[] indices = cps.stream().mapToInt(ChangePoint::index).toArray();
        assertArrayEquals(new int[]{10}, indices,
                "original algorithm should detect change point at [10], got " + java.util.Arrays.toString(indices));
    }

    @Test
    void ttestNotSignificantForStableData() {
        // Matches test_significance_tester (first case)
        double[] stable = {1.00, 1.02, 1.05, 0.95, 0.98, 1.00, 1.02, 1.05, 0.95, 0.98};
        TTestSignificanceTester tester = new TTestSignificanceTester(0.001);
        ChangePoint candidate = ChangePoint.candidate(5, 0.0);
        int[][] intervals = {{0, stable.length}};

        ChangePoint cp = tester.test(candidate, stable, intervals);
        assertFalse(tester.isSignificant(cp), "stable data should not be significant");
        assertTrue(cp.pvalue() > 0.99, "pvalue should be close to 1.0, was " + cp.pvalue());
    }

    @Test
    void ttestSignificantForShiftedData() {
        // Matches test_significance_tester (second case)
        double[] shifted = {1.00, 1.02, 1.05, 0.95, 0.98, 0.80, 0.82, 0.85, 0.79, 0.77};
        TTestSignificanceTester tester = new TTestSignificanceTester(0.001);
        ChangePoint candidate = ChangePoint.candidate(5, 0.0);
        int[][] intervals = {{0, shifted.length}};

        ChangePoint cp = tester.test(candidate, shifted, intervals);
        assertTrue(tester.isSignificant(cp), "shifted data should be significant");
        assertTrue(cp.pvalue() < 0.001, "pvalue should be < 0.001, was " + cp.pvalue());
    }
}
