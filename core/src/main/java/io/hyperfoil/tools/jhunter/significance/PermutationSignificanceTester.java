package io.hyperfoil.tools.jhunter.significance;

import io.hyperfoil.tools.jhunter.ChangePoint;
import io.hyperfoil.tools.jhunter.calculator.PairDistanceCalculator;

import java.util.Random;

public class PermutationSignificanceTester extends SignificanceTester {

    private final int permutations;
    private final Random random;

    public PermutationSignificanceTester(double maxPvalue, int permutations, long seed) {
        super(maxPvalue);
        this.permutations = permutations;
        this.random = new Random(seed);
    }

    public PermutationSignificanceTester(double maxPvalue, int permutations) {
        super(maxPvalue);
        this.permutations = permutations;
        this.random = new Random();
    }

    @Override
    public ChangePoint test(ChangePoint candidate, double[] series, int[][] intervals) {
        int extremeCount = 0;

        for (int p = 0; p < permutations; p++) {
            double[] shuffled = series.clone();
            for (int[] interval : intervals) {
                shuffleRange(shuffled, interval[0], interval[1]);
            }

            PairDistanceCalculator calc = new PairDistanceCalculator(shuffled);
            ChangePoint permCandidate = calc.getNextCandidate(intervals);
            if (permCandidate != null && permCandidate.qhat() >= candidate.qhat()) {
                extremeCount++;
            }
        }

        double pvalue = (extremeCount + 1.0) / (permutations + 1.0);
        return candidate.withStats(pvalue, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    private void shuffleRange(double[] array, int start, int end) {
        for (int i = end - 1; i > start; i--) {
            int j = start + random.nextInt(i - start + 1);
            double tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
}
