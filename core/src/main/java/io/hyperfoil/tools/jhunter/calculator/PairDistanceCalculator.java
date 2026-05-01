package io.hyperfoil.tools.jhunter.calculator;

import io.hyperfoil.tools.jhunter.ChangePoint;

public class PairDistanceCalculator extends Calculator {

    private final double power;
    private final boolean powerIsOne;
    private double[] V;
    private double[][] H;

    public PairDistanceCalculator(double[] series) {
        this(series, 1.0);
    }

    public PairDistanceCalculator(double[] series, double power) {
        super(series);
        if (power <= 0 || power >= 2) {
            throw new IllegalArgumentException("power=" + power + " isn't in (0, 2)");
        }
        this.power = power;
        this.powerIsOne = (power == 1.0);
    }

    private double distance(int i, int j) {
        double diff = Math.abs(series[i] - series[j]);
        return powerIsOne ? diff : Math.pow(diff, power);
    }

    private void computeVH() {
        int n = series.length;

        V = new double[n - 1];
        for (int j = 1; j < n; j++) {
            double sum = 0;
            for (int i = 0; i < j; i++) {
                sum += distance(i, j);
            }
            V[j - 1] = sum;
        }

        H = new double[n - 1][n - 1];
        for (int i = 0; i < n - 1; i++) {
            double cumsum = 0;
            for (int j = i; j < n - 1; j++) {
                cumsum += distance(i, j + 1);
                H[i][j] = cumsum;
            }
        }
    }

    private double[][] getQVals(int start, int end) {
        if (V == null || H == null) {
            computeVH();
        }

        int size = end - 1 - start;
        if (size <= 0) return new double[0][0];

        // Compute cumsumV directly: cumulative sum of local V (V adjusted for start offset)
        double[] cumsumV = new double[size];
        double cumV = 0;
        for (int j = 0; j < size; j++) {
            double correction = 0;
            for (int i = 0; i < start; i++) {
                correction += distance(i, start + 1 + j);
            }
            cumV += V[start + j] - correction;
            cumsumV[j] = cumV;
        }

        // Compute cumsumH directly: cumulative sum of local H along rows (axis 0)
        double[][] cumsumH = new double[size][size];
        for (int j = 0; j < size; j++) {
            cumsumH[0][j] = H[start][start + j];
            for (int i = 1; i < size; i++) {
                cumsumH[i][j] = cumsumH[i - 1][j] + H[start + i][start + j];
            }
        }

        double[][] Q = new double[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = i; j < size; j++) {
                int tau = i + 1 + start;
                int kappa = j + 2 + start;

                double cumsumVi = (i > 0) ? cumsumV[i - 1] : 0;
                double A = 2.0 / (kappa - start) * (cumsumH[i][j] - cumsumVi);

                double B = 0;
                if (i > 0 && (tau - start - 1) > 0) {
                    B = 2.0 * (kappa - tau) / ((kappa - start) * (tau - start - 1.0)) * cumsumV[i - 1];
                }

                double C = 0;
                if (j > i && (kappa - tau - 1) > 0) {
                    int upper = Math.min(j, size - 1);
                    double hSum = cumsumH[upper][j] - cumsumH[i][j];
                    C = 2.0 * (tau - start) / ((kappa - start) * (kappa - tau - 1.0)) * hSum;
                }

                Q[i][j] = A - B - C;
            }
        }

        return Q;
    }

    @Override
    public ChangePoint getCandidateChangePoint(int start, int end) {
        if (end - start <= 1) {
            throw new IllegalArgumentException("interval must have length > 1, but [" + start + ":" + end + "] was given");
        }

        double[][] Q = getQVals(start, end);
        int bestI = 0, bestJ = 0;
        double bestQ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < Q.length; i++) {
            for (int j = i; j < Q[i].length; j++) {
                if (Q[i][j] > bestQ) {
                    bestQ = Q[i][j];
                    bestI = i;
                    bestJ = j;
                }
            }
        }
        return ChangePoint.candidate(bestI + 1 + start, bestQ);
    }
}
