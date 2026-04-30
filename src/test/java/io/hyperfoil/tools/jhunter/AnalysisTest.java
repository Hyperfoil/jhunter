package io.hyperfoil.tools.jhunter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisTest {

    @Test
    void detectsStepChange() {
        // 50 values at 10.0, then 50 values at 20.0
        double[] series = new double[100];
        for (int i = 0; i < 50; i++) series[i] = 10.0;
        for (int i = 50; i < 100; i++) series[i] = 20.0;

        List<ChangePoint> points = Analysis.computeChangePoints(series, new AnalysisOptions());
        assertFalse(points.isEmpty(), "should detect at least one change point");
        assertEquals(50, points.get(0).index(), "change point should be at index 50");
    }

    @Test
    void noFalsePositivesOnStableData() {
        double[] series = new double[100];
        for (int i = 0; i < 100; i++) series[i] = 42.0;

        List<ChangePoint> points = Analysis.computeChangePoints(series, new AnalysisOptions());
        assertTrue(points.isEmpty(), "should not detect change points in constant data");
    }

    @Test
    void detectsMultipleChangePoints() {
        // Three plateaus: 10, 30, 15
        double[] series = new double[150];
        for (int i = 0; i < 50; i++) series[i] = 10.0;
        for (int i = 50; i < 100; i++) series[i] = 30.0;
        for (int i = 100; i < 150; i++) series[i] = 15.0;

        List<ChangePoint> points = Analysis.computeChangePoints(series, new AnalysisOptions());
        assertEquals(2, points.size(), "should detect two change points");
        assertEquals(50, points.get(0).index());
        assertEquals(100, points.get(1).index());
    }

    @Test
    void handlesNoisyData() {
        // Large step change with noise — mean shift of 10 with std ~1
        Random rng = new Random(42);
        double[] series = new double[100];
        for (int i = 0; i < 50; i++) series[i] = 10.0 + rng.nextGaussian();
        for (int i = 50; i < 100; i++) series[i] = 20.0 + rng.nextGaussian();

        List<ChangePoint> points = Analysis.computeChangePoints(series, new AnalysisOptions(0.01));
        assertFalse(points.isEmpty(), "should detect change point in noisy step data");
        // Find the change point with the largest magnitude
        ChangePoint best = points.stream()
                .max((a, b) -> Double.compare(a.magnitude(), b.magnitude()))
                .orElseThrow();
        assertTrue(Math.abs(best.index() - 50) <= 5,
                "change point should be near index 50 but was " + best.index());
    }

    @Test
    void shortSeriesReturnsEmpty() {
        double[] series = {1.0, 2.0};
        List<ChangePoint> points = Analysis.computeChangePoints(series, new AnalysisOptions());
        assertTrue(points.isEmpty());
    }

    @Test
    void changePointHasCorrectStats() {
        double[] series = new double[100];
        for (int i = 0; i < 50; i++) series[i] = 10.0;
        for (int i = 50; i < 100; i++) series[i] = 20.0;

        List<ChangePoint> points = Analysis.computeChangePoints(series, new AnalysisOptions());
        assertFalse(points.isEmpty());

        ChangePoint cp = points.get(0);
        assertEquals(10.0, cp.meanBefore(), 0.01);
        assertEquals(20.0, cp.meanAfter(), 0.01);
        assertTrue(cp.pvalue() < 0.001, "pvalue should be very small for clear step change");
        assertEquals(1.0, cp.forwardChangePercent(), 0.01, "should be 100% increase");
    }

    @Test
    void originalAlgorithmDetectsStepChange() {
        double[] series = new double[60];
        for (int i = 0; i < 30; i++) series[i] = 10.0;
        for (int i = 30; i < 60; i++) series[i] = 20.0;

        List<ChangePoint> points = Analysis.computeChangePointsOriginal(series, 0.01, 42);
        assertFalse(points.isEmpty(), "original algorithm should detect step change");
        assertEquals(30, points.get(0).index());
    }
}
