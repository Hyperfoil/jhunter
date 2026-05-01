package io.hyperfoil.tools.jhunter;

import io.hyperfoil.tools.jhunter.calculator.PairDistanceCalculator;
import io.hyperfoil.tools.jhunter.math.TDistribution;
import io.hyperfoil.tools.jhunter.significance.TTestSignificanceTester;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class EdgeCaseTest {

    // --- Series length edge cases ---

    @Test
    void emptySeriesReturnsEmpty() {
        assertEquals(List.of(), Analysis.computeChangePoints(new double[0], new AnalysisOptions()));
    }

    @Test
    void singleElementReturnsEmpty() {
        assertEquals(List.of(), Analysis.computeChangePoints(new double[]{42.0}, new AnalysisOptions()));
    }

    @Test
    void twoElementsReturnsEmpty() {
        assertEquals(List.of(), Analysis.computeChangePoints(new double[]{1.0, 100.0}, new AnalysisOptions()));
    }

    @Test
    void threeElementsWithChange() {
        PairDistanceCalculator calc = new PairDistanceCalculator(new double[]{0, 0, 100});
        ChangePoint cp = calc.getCandidateChangePoint(0, 3);
        assertEquals(2, cp.index());
        assertTrue(cp.qhat() > 0);
    }

    @Test
    void minimumViableSeries() {
        // 3+3 elements with clear step
        double[] series = {1, 1, 1, 100, 100, 100};
        PairDistanceCalculator calc = new PairDistanceCalculator(series);
        TTestSignificanceTester tester = new TTestSignificanceTester(0.05);
        ChangePointDetector detector = new ChangePointDetector(calc, tester);
        List<ChangePoint> cps = detector.detect(series);
        assertFalse(cps.isEmpty());
        assertEquals(3, cps.get(0).index());
    }

    // --- Data pattern edge cases ---

    @Test
    void allIdenticalValues() {
        double[] series = new double[50];
        Arrays.fill(series, 7.0);
        List<ChangePoint> cps = Analysis.computeChangePoints(series, new AnalysisOptions());
        assertTrue(cps.isEmpty(), "identical values should have no change points");
    }

    @Test
    void linearTrend() {
        // Gradual linear increase — no abrupt change
        double[] series = new double[100];
        for (int i = 0; i < 100; i++) series[i] = i * 0.1;
        List<ChangePoint> cps = Analysis.computeChangePoints(series, new AnalysisOptions());
        // May find some points, but they should have low magnitude
        for (ChangePoint cp : cps) {
            assertTrue(cp.magnitude() < 5.0,
                    "linear trend change points should have small magnitude, got " + cp.magnitude());
        }
    }

    @Test
    void singleSpike() {
        // One outlier in otherwise stable data
        double[] series = new double[50];
        Arrays.fill(series, 10.0);
        series[25] = 1000.0;

        PairDistanceCalculator calc = new PairDistanceCalculator(series);
        TTestSignificanceTester tester = new TTestSignificanceTester(0.001);
        ChangePointDetector detector = new ChangePointDetector(calc, tester);
        List<ChangePoint> cps = detector.detect(series);
        // A single spike may or may not be detected — but it shouldn't crash
        assertNotNull(cps);
    }

    @Test
    void negativeValues() {
        double[] series = new double[60];
        for (int i = 0; i < 30; i++) series[i] = -100.0;
        for (int i = 30; i < 60; i++) series[i] = -50.0;

        PairDistanceCalculator calc = new PairDistanceCalculator(series);
        TTestSignificanceTester tester = new TTestSignificanceTester(0.01);
        ChangePointDetector detector = new ChangePointDetector(calc, tester);
        List<ChangePoint> cps = detector.detect(series);
        assertFalse(cps.isEmpty());
        assertEquals(30, cps.get(0).index());
    }

    @Test
    void veryLargeValues() {
        double[] series = new double[60];
        for (int i = 0; i < 30; i++) series[i] = 1e12;
        for (int i = 30; i < 60; i++) series[i] = 2e12;

        PairDistanceCalculator calc = new PairDistanceCalculator(series);
        TTestSignificanceTester tester = new TTestSignificanceTester(0.01);
        ChangePointDetector detector = new ChangePointDetector(calc, tester);
        List<ChangePoint> cps = detector.detect(series);
        assertFalse(cps.isEmpty());
        assertEquals(30, cps.get(0).index());
    }

    @Test
    void verySmallDifference() {
        // Difference is tiny relative to noise — should not be detected
        Random rng = new Random(7);
        double[] series = new double[60];
        for (int i = 0; i < 60; i++) series[i] = 10.0 + rng.nextGaussian() * 2.0;

        List<ChangePoint> cps = Analysis.computeChangePoints(series, new AnalysisOptions(0.001));
        assertTrue(cps.isEmpty(), "tiny difference buried in noise should not be detected");
    }

    @Test
    void alternatingValues() {
        // Alternating high/low — no persistent change
        double[] series = new double[60];
        for (int i = 0; i < 60; i++) series[i] = (i % 2 == 0) ? 10.0 : 20.0;

        List<ChangePoint> cps = Analysis.computeChangePoints(series, new AnalysisOptions());
        assertTrue(cps.isEmpty(), "alternating pattern should not have change points");
    }

    @Test
    void multipleSteps() {
        // Staircase: 10 → 20 → 30 → 40
        double[] series = new double[80];
        for (int i = 0; i < 20; i++) series[i] = 10.0;
        for (int i = 20; i < 40; i++) series[i] = 20.0;
        for (int i = 40; i < 60; i++) series[i] = 30.0;
        for (int i = 60; i < 80; i++) series[i] = 40.0;

        PairDistanceCalculator calc = new PairDistanceCalculator(series);
        TTestSignificanceTester tester = new TTestSignificanceTester(0.01);
        ChangePointDetector detector = new ChangePointDetector(calc, tester);
        List<ChangePoint> cps = detector.detect(series);

        assertEquals(3, cps.size(), "should detect 3 change points in staircase");
        int[] indices = cps.stream().mapToInt(ChangePoint::index).sorted().toArray();
        assertArrayEquals(new int[]{20, 40, 60}, indices);
    }

    @Test
    void changeAtVeryStart() {
        // Change between element 1 and 2
        double[] series = new double[20];
        series[0] = 0;
        for (int i = 1; i < 20; i++) series[i] = 100.0;

        PairDistanceCalculator calc = new PairDistanceCalculator(series);
        ChangePoint cp = calc.getCandidateChangePoint(0, 20);
        assertEquals(1, cp.index(), "should detect change at index 1");
    }

    @Test
    void changeAtVeryEnd() {
        // Change near the end
        double[] series = new double[20];
        for (int i = 0; i < 19; i++) series[i] = 10.0;
        series[19] = 100.0;

        PairDistanceCalculator calc = new PairDistanceCalculator(series);
        ChangePoint cp = calc.getCandidateChangePoint(0, 20);
        assertEquals(19, cp.index(), "should detect change at index 19");
    }

    // --- TDistribution edge cases ---

    @Test
    void tDistributionSymmetry() {
        double left = TDistribution.cdf(-2.0, 10);
        double right = TDistribution.cdf(2.0, 10);
        assertEquals(1.0, left + right, 1e-10, "CDF should be symmetric");
    }

    @Test
    void tDistributionKnownValues() {
        // t=0, any df → CDF should be 0.5
        assertEquals(0.5, TDistribution.cdf(0, 10), 1e-10);
        assertEquals(0.5, TDistribution.cdf(0, 1), 1e-10);
        assertEquals(0.5, TDistribution.cdf(0, 100), 1e-10);
    }

    @Test
    void tDistributionExtremeT() {
        assertTrue(TDistribution.cdf(100, 10) > 0.999);
        assertTrue(TDistribution.cdf(-100, 10) < 0.001);
    }

    @Test
    void tDistributionSmallDf() {
        // df=1 is the Cauchy distribution
        double cdf = TDistribution.cdf(1.0, 1);
        assertEquals(0.75, cdf, 0.01, "t=1, df=1 (Cauchy) should give CDF ≈ 0.75");
    }

    @Test
    void welchTWithEqualGroups() {
        double t = TDistribution.welchT(10.0, 2.0, 30, 12.0, 2.0, 30);
        assertTrue(t < 0, "group 1 mean < group 2 mean → t should be negative");
        double pval = TDistribution.twoTailedPvalue(t, TDistribution.welchDf(2.0, 30, 2.0, 30));
        assertTrue(pval < 0.001, "clear difference should have small pvalue");
    }

    @Test
    void welchTIdenticalGroups() {
        double t = TDistribution.welchT(10.0, 2.0, 30, 10.0, 2.0, 30);
        assertEquals(0.0, t, 1e-15, "identical groups should have t=0");
        double pval = TDistribution.twoTailedPvalue(t, TDistribution.welchDf(2.0, 30, 2.0, 30));
        assertEquals(1.0, pval, 1e-10, "identical groups should have pvalue=1");
    }

    // --- Calculator edge cases ---

    @Test
    void calculatorWithPowerHalf() {
        double[] series = {0, 0, 0, 10, 10, 10};
        PairDistanceCalculator calc = new PairDistanceCalculator(series, 0.5);
        ChangePoint cp = calc.getCandidateChangePoint(0, 6);
        assertEquals(3, cp.index());
        assertTrue(cp.qhat() > 0);
    }

    @Test
    void calculatorInvalidPowerThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PairDistanceCalculator(new double[]{1, 2, 3}, 0.0));
        assertThrows(IllegalArgumentException.class, () ->
                new PairDistanceCalculator(new double[]{1, 2, 3}, 2.0));
        assertThrows(IllegalArgumentException.class, () ->
                new PairDistanceCalculator(new double[]{1, 2, 3}, -1.0));
    }

    @Test
    void calculatorSubinterval() {
        // Only analyze a subrange
        double[] series = {1, 1, 1, 1, 1, 100, 100, 100, 100, 100};
        PairDistanceCalculator calc = new PairDistanceCalculator(series);

        // Full range
        ChangePoint full = calc.getCandidateChangePoint(0, 10);
        assertEquals(5, full.index());

        // Only first half — no change
        ChangePoint firstHalf = calc.getCandidateChangePoint(0, 5);
        assertTrue(firstHalf.qhat() == 0 || firstHalf.qhat() < full.qhat(),
                "first half should have lower qhat than full");
    }

    // --- ChangePoint stats ---

    @Test
    void changePointMagnitudeWithZeroMean() {
        ChangePoint cp = new ChangePoint(5, 1.0, 0.001, 0.0, 10.0, 1.0, 1.0);
        assertTrue(Double.isNaN(cp.forwardChangePercent()), "forward change from 0 should be NaN");
        // backward: (0/10) - 1 = -1.0, magnitude = abs(-1.0) = 1.0
        assertEquals(1.0, cp.magnitude(), 0.01, "magnitude should be 1.0 (100% change)");
    }

    @Test
    void changePointMagnitudeNormal() {
        ChangePoint cp = new ChangePoint(5, 1.0, 0.001, 10.0, 20.0, 1.0, 1.0);
        assertEquals(1.0, cp.forwardChangePercent(), 0.01, "10 → 20 is 100% increase");
        assertEquals(-0.5, cp.backwardChangePercent(), 0.01, "20 → 10 is -50% (decrease)");
        assertEquals(1.0, cp.magnitude(), 0.01, "magnitude should be max(abs(1.0), abs(-0.5))");
    }

    // --- Reproducibility ---

    @Test
    void deterministicResults() {
        double[] series = new double[100];
        Random rng = new Random(123);
        for (int i = 0; i < 50; i++) series[i] = 10.0 + rng.nextGaussian();
        rng = new Random(456);
        for (int i = 50; i < 100; i++) series[i] = 20.0 + rng.nextGaussian();

        List<ChangePoint> run1 = Analysis.computeChangePoints(series, new AnalysisOptions(0.01));
        List<ChangePoint> run2 = Analysis.computeChangePoints(series, new AnalysisOptions(0.01));

        assertEquals(run1.size(), run2.size(), "same input should produce same number of change points");
        for (int i = 0; i < run1.size(); i++) {
            assertEquals(run1.get(i).index(), run2.get(i).index(), "indices should match");
            assertEquals(run1.get(i).pvalue(), run2.get(i).pvalue(), 1e-15, "pvalues should match");
        }
    }
}
