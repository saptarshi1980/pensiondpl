package com.example;

import java.util.ArrayList;
import java.util.List;

public class PensionCalculator {

    public static ComparisonResult calculateAndCompare(double lumpSum, double monthlyPension,
                                                       double fdAnnualRate, double rdAnnualRate) {
        double fdMonthlyRate = fdAnnualRate / 100 / 12;
        double rdMonthlyRate = rdAnnualRate / 100 / 12;

        double fdValue = lumpSum;
        double rdValue = 0;
        int months = 0;
        int years = 0;

        List<YearlyComparison> yearlyData = new ArrayList<>();

        while (rdValue < fdValue) {
            months++;

            fdValue *= (1 + fdMonthlyRate);
            rdValue += monthlyPension;
            rdValue *= (1 + rdMonthlyRate);

            if (months % 12 == 0) {
                years++;
                double difference = rdValue - fdValue;
                String status = difference < 0 ? "FD Better" : "Pension+RD Better";
                yearlyData.add(new YearlyComparison(years, rdValue, fdValue, difference, status));
            }

            if (months > 1200) {
                return null;
            }
        }

        if (months % 12 != 0) {
            double difference = rdValue - fdValue;
            yearlyData.add(new YearlyComparison(months / 12.0, rdValue, fdValue, difference, "BREAK-EVEN"));
        }

        return new ComparisonResult(months, rdValue, fdValue, yearlyData);
    }
}
