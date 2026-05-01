package io.hyperfoil.tools.jhunter.math;

public final class TDistribution {

    private TDistribution() {}

    public static double cdf(double t, double df) {
        double x = df / (df + t * t);
        double prob = 0.5 * regularizedIncompleteBeta(df / 2.0, 0.5, x);
        return t >= 0 ? 1.0 - prob : prob;
    }

    public static double twoTailedPvalue(double t, double df) {
        if (Double.isInfinite(t)) return 0.0;
        if (Double.isNaN(t) || Double.isNaN(df) || df <= 0) return 1.0;
        double oneTail = cdf(-Math.abs(t), df);
        return 2.0 * oneTail;
    }

    public static double welchT(double mean1, double std1, int n1, double mean2, double std2, int n2) {
        double se = Math.sqrt(std1 * std1 / n1 + std2 * std2 / n2);
        if (se == 0) {
            if (mean1 == mean2) return 0;
            return mean1 > mean2 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        return (mean1 - mean2) / se;
    }

    public static double welchDf(double std1, int n1, double std2, int n2) {
        double v1 = std1 * std1 / n1;
        double v2 = std2 * std2 / n2;
        double num = (v1 + v2) * (v1 + v2);
        double den = v1 * v1 / (n1 - 1) + v2 * v2 / (n2 - 1);
        if (den == 0) return Math.max(n1 + n2 - 2, 1);
        return num / den;
    }

    static double regularizedIncompleteBeta(double a, double b, double x) {
        if (x < 0 || x > 1) return Double.NaN;
        if (x == 0) return 0;
        if (x == 1) return 1;

        if (x > (a + 1) / (a + b + 2)) {
            return 1.0 - regularizedIncompleteBeta(b, a, 1.0 - x);
        }

        double lnBeta = logBeta(a, b);
        double prefix = Math.exp(a * Math.log(x) + b * Math.log(1.0 - x) - lnBeta) / a;

        // Lentz's continued fraction
        double c = 1.0;
        double d = 1.0 - (a + b) * x / (a + 1.0);
        if (Math.abs(d) < 1e-30) d = 1e-30;
        d = 1.0 / d;
        double result = d;

        for (int m = 1; m <= 200; m++) {
            // Even step
            double numerator = m * (b - m) * x / ((a + 2.0 * m - 1.0) * (a + 2.0 * m));
            d = 1.0 + numerator * d;
            if (Math.abs(d) < 1e-30) d = 1e-30;
            c = 1.0 + numerator / c;
            if (Math.abs(c) < 1e-30) c = 1e-30;
            d = 1.0 / d;
            result *= d * c;

            // Odd step
            numerator = -(a + m) * (a + b + m) * x / ((a + 2.0 * m) * (a + 2.0 * m + 1.0));
            d = 1.0 + numerator * d;
            if (Math.abs(d) < 1e-30) d = 1e-30;
            c = 1.0 + numerator / c;
            if (Math.abs(c) < 1e-30) c = 1e-30;
            d = 1.0 / d;
            double delta = d * c;
            result *= delta;

            if (Math.abs(delta - 1.0) < 1e-15) break;
        }

        return prefix * result;
    }

    static double logBeta(double a, double b) {
        return logGamma(a) + logGamma(b) - logGamma(a + b);
    }

    static double logGamma(double x) {
        // Lanczos approximation
        double[] coefficients = {
                676.5203681218851,
                -1259.1392167224028,
                771.32342877765313,
                -176.61502916214059,
                12.507343278686905,
                -0.13857109526572012,
                9.9843695780195716e-6,
                1.5056327351493116e-7
        };

        if (x < 0.5) {
            return Math.log(Math.PI / Math.sin(Math.PI * x)) - logGamma(1.0 - x);
        }

        x -= 1;
        double a = 0.99999999999980993;
        double t = x + 7.5;
        for (int i = 0; i < coefficients.length; i++) {
            a += coefficients[i] / (x + i + 1);
        }

        return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }
}
