package io.hyperfoil.tools.jhunter.benchmark;

import io.hyperfoil.tools.jhunter.ChangePoint;
import io.hyperfoil.tools.jhunter.calculator.PairDistanceCalculator;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class PairDistanceCalculatorBenchmark {

    @Param({"50", "100", "500", "1000", "2000"})
    int size;

    double[] series;

    @Setup
    public void setup() {
        Random rng = new Random(42);
        series = new double[size];
        int mid = size / 2;
        for (int i = 0; i < mid; i++) series[i] = 10.0 + rng.nextGaussian();
        for (int i = mid; i < size; i++) series[i] = 20.0 + rng.nextGaussian();
    }

    @Benchmark
    public ChangePoint getCandidateChangePoint() {
        PairDistanceCalculator calc = new PairDistanceCalculator(series);
        return calc.getCandidateChangePoint(0, series.length);
    }
}
