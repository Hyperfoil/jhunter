package io.hyperfoil.tools.jhunter.benchmark;

import io.hyperfoil.tools.jhunter.Analysis;
import io.hyperfoil.tools.jhunter.AnalysisOptions;
import io.hyperfoil.tools.jhunter.ChangePoint;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class ChangePointDetectionBenchmark {

    @Param({"100", "500", "1000", "5000"})
    int size;

    double[] series;
    AnalysisOptions options;

    @Setup
    public void setup() {
        Random rng = new Random(42);
        series = new double[size];
        int mid = size / 2;
        for (int i = 0; i < mid; i++) series[i] = 10.0 + rng.nextGaussian();
        for (int i = mid; i < size; i++) series[i] = 20.0 + rng.nextGaussian();
        options = new AnalysisOptions();
    }

    @Benchmark
    public List<ChangePoint> windowed(Blackhole bh) {
        return Analysis.computeChangePoints(series, options);
    }

    @Benchmark
    public List<ChangePoint> original() {
        return Analysis.computeChangePointsOriginal(series, 0.01, 42);
    }
}
