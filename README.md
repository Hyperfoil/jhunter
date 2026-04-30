# jhunter

A Java library for detecting statistically significant change points in time series data. 
Implements the Hunter/E-Divisive algorithm, ported from [Apache Otava](https://github.com/apache/otava).

Zero external dependencies. Java 21+.

## What it does

Given a time series of performance measurements (e.g., throughput, latency, response time),
jhunter finds the points where the distribution changes — indicating a regression, improvement,
or other significant shift. Unlike fixed thresholds, it adapts automatically to the level of
noise in the data and only reports persistent, statistically significant changes.

## Quick start

```xml
<dependency>
    <groupId>io.hyperfoil.tools</groupId>
    <artifactId>jhunter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
import io.hyperfoil.tools.jhunter.Analysis;
import io.hyperfoil.tools.jhunter.AnalysisOptions;
import io.hyperfoil.tools.jhunter.ChangePoint;

double[] series = {
    1.02, 0.95, 0.99, 1.00, 1.12,
    1.00, 1.01, 0.98, 1.01, 0.96,
    0.50, 0.51, 0.48, 0.48, 0.55,
    0.50, 0.49, 0.51, 0.50, 0.49
};

List<ChangePoint> changePoints = Analysis.computeChangePoints(series, new AnalysisOptions());

for (ChangePoint cp : changePoints) {
    System.out.printf("Change at index %d: %.2f → %.2f (p=%.6f, magnitude=%.1f%%)%n",
        cp.index(), cp.meanBefore(), cp.meanAfter(), cp.pvalue(), cp.magnitude() * 100);
}
// Output: Change at index 10: 1.00 → 0.50 (p=0.000000, magnitude=100.0%)
```

## API

### `Analysis` — Main entry point

```java
// Hunter's two-step algorithm (recommended — fast and accurate)
List<ChangePoint> cps = Analysis.computeChangePoints(double[] series, AnalysisOptions options);

// Original Matteson-James algorithm (slower, uses permutation testing)
List<ChangePoint> cps = Analysis.computeChangePointsOriginal(double[] series, double maxPvalue, long seed);
```

### `AnalysisOptions` — Configuration

```java
new AnalysisOptions(
    int windowLen,      // sliding window size (default: 50)
    double maxPvalue,   // significance threshold (default: 0.001)
    double minMagnitude // minimum relative change to report (default: 0.0)
);

// Convenience constructors
new AnalysisOptions();           // all defaults
new AnalysisOptions(0.01);       // custom pvalue, other defaults
```

| Parameter | Default | Description |
|-----------|---------|-------------|
| `windowLen` | 50 | Sliding window size for the split phase. Larger windows detect broader shifts but may miss local changes. |
| `maxPvalue` | 0.001 | Significance threshold. Lower = fewer false positives, higher = more sensitive. A value of 0.001 means roughly 0.1% false positive rate. |
| `minMagnitude` | 0.0 | Minimum relative change magnitude to report. 0.1 means at least 10% change. |

### `ChangePoint` — Result

```java
record ChangePoint(
    int index,         // position in the series where the change occurs
    double qhat,       // Q-hat divergence statistic (higher = more distinct)
    double pvalue,     // statistical significance (lower = more significant)
    double meanBefore,
    double meanAfter,
    double stdBefore,
    double stdAfter
)
```

**Derived methods:**

| Method | Description |
|--------|-------------|
| `forwardChangePercent()` | Relative change: `(meanAfter / meanBefore) - 1` |
| `backwardChangePercent()` | Reverse relative change: `(meanBefore / meanAfter) - 1` |
| `magnitude()` | `max(abs(forward), abs(backward))` — symmetric measure of change size |

### Lower-level API

For custom pipelines, the components can be used independently:

```java
// Direct Q-hat calculation
PairDistanceCalculator calc = new PairDistanceCalculator(series);
ChangePoint candidate = calc.getCandidateChangePoint(0, series.length);

// Custom significance testing
TTestSignificanceTester tester = new TTestSignificanceTester(0.01);
ChangePoint tested = tester.test(candidate, series, intervals);

// Custom detection loop
ChangePointDetector detector = new ChangePointDetector(calc, tester);
List<ChangePoint> cps = detector.detect(series);
```

## Algorithm

jhunter implements two change point detection algorithms:

### Hunter's two-step algorithm (default)

1. **Split**: Slides a window across the series with a relaxed significance threshold,
   finding candidate ("weak") change points using the E-Divisive Q-hat statistic.

2. **Merge**: Filters candidates using strict significance (Welch's t-test) and
   magnitude thresholds, iteratively removing the weakest point until all remaining
   points meet the criteria.

This is much faster than the original algorithm for long series because each window
is small (default 50 points).

### Original Matteson-James algorithm

Applies the E-Divisive algorithm to the full series with permutation-based
significance testing. More theoretically sound but O(n² × permutations) per change
point. Use `Analysis.computeChangePointsOriginal()` for this variant.

### References

- Matteson, D.S. and James, N.A. (2014). "A Nonparametric Approach for Multiple Change Point Analysis of Multivariate Data." *Journal of the American Statistical Association*.
- [Apache Otava](https://github.com/apache/otava) — the Python implementation this library is ported from.

## Building

```bash
mvn clean test
```

Requires Java 21+. No external dependencies (only JUnit 5 for tests).

## License

Apache License 2.0
