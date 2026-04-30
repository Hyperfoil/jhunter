package io.hyperfoil.tools.jhunter.calculator;

import io.hyperfoil.tools.jhunter.ChangePoint;

public class PairDistanceCalculator extends Calculator {

    private final double power;
    private double[] V;
    private double[][] H;
    private double[][] distances;

    public PairDistanceCalculator(double[] series) {
        this(series, 1.0);
    }

    public PairDistanceCalculator(double[] series, double power) {
        super(series);
        if (power <= 0 || power >= 2) {
            throw new IllegalArgumentException("power=" + power + " isn't in (0, 2)");
        }
        this.power = power;
    }

    private void calculatePairwiseDistances() {
        int n = series.length;
        distances = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double d = Math.pow(Math.abs(series[i] - series[j]), power);
                distances[i][j] = d;
                distances[j][i] = d;
            }
        }

        // triu = upper triangle of distances with k=1, rows [0..n-2], cols [1..n-1]
        // V = column sums of triu (sum along axis 0)
        V = new double[n - 1];
        for (int j = 1; j < n; j++) {
            double sum = 0;
            for (int i = 0; i < j; i++) {
                sum += distances[i][j];
            }
            V[j - 1] = sum;
        }

        // H = row-wise cumulative sum of triu
        // H[i][j] = sum of distances[i][i+1..j+1] (upper triangle, cumulative along columns)
        H = new double[n - 1][n - 1];
        for (int i = 0; i < n - 1; i++) {
            double cumsum = 0;
            for (int j = i; j < n - 1; j++) {
                cumsum += distances[i][j + 1];
                H[i][j] = cumsum;
            }
        }
    }

    private double[][] getQVals(int start, int end) {
        if (V == null || H == null) {
            calculatePairwiseDistances();
        }

        int size = end - 1 - start;
        if (size <= 0) return new double[0][0];

        // Compute local V: subtract contributions from rows [0, start)
        double[] localV = new double[size];
        for (int j = 0; j < size; j++) {
            double correction = 0;
            for (int i = 0; i < start; i++) {
                correction += distances[i][start + 1 + j];
            }
            localV[j] = V[start + j] - correction;
        }

        // Cumulative sum of localV
        double[] cumsumV = new double[size];
        cumsumV[0] = localV[0];
        for (int i = 1; i < size; i++) {
            cumsumV[i] = cumsumV[i - 1] + localV[i];
        }

        // Local H: H[start..end-2][start..end-2]
        // Compute cumulative sum of local H along rows (axis 0)
        double[][] localH = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                localH[i][j] = H[start + i][start + j];
            }
        }
        double[][] cumsumH = new double[size][size];
        for (int j = 0; j < size; j++) {
            cumsumH[0][j] = localH[0][j];
            for (int i = 1; i < size; i++) {
                cumsumH[i][j] = cumsumH[i - 1][j] + localH[i][j];
            }
        }

        // Compute A, B, C matrices
        double[][] Q = new double[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = i; j < size; j++) {
                int tau = i + 1 + start;
                int kappa = j + 2 + start;

                // A = 2/(kappa - start) * (cumsumH[i][j] - cumsumV_at_i)
                double cumsumVi = (i > 0) ? cumsumV[i - 1] : 0;
                double A = 2.0 / (kappa - start) * (cumsumH[i][j] - cumsumVi);

                // B = 2*(kappa-tau) / ((kappa-start)*(tau-start-1)) * cumsumV[i-1]
                double B = 0;
                if (i > 0 && (tau - start - 1) > 0) {
                    B = 2.0 * (kappa - tau) / ((kappa - start) * (tau - start - 1.0)) * cumsumV[i - 1];
                }

                // C = 2*(tau-start) / ((kappa-start)*(kappa-tau-1)) * reverse_cumsumH
                double C = 0;
                if (j > i && (kappa - tau - 1) > 0) {
                    // Reverse cumulative sum of H from row i+1 to row size-1, at column j
                    double revCumsumH = cumsumH[size - 1][j] - cumsumH[i][j];
                    // But we need the reversed cumsum: sum from row (i+1) to end at col j
                    // minus sum from row (i+1) to end at cols < j... actually we need
                    // sum of H[i+1..j-1][j] which is the flipud cumsum pattern
                    // Simpler: directly compute the needed sum
                    double hSum = 0;
                    for (int t = i + 1; t < size && t <= j; t++) {
                        hSum += localH[t][j];
                    }
                    C = 2.0 * (tau - start) / ((kappa - start) * (kappa - tau - 1.0)) * hSum;
                }

                Q[i][j] = A - B - C;
            }
        }

        // Zero out lower triangle
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < i; j++) {
                Q[i][j] = 0;
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
