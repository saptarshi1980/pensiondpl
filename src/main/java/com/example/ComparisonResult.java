package com.example;

import java.util.List;

public class ComparisonResult {
    public int months;
    public double rdValue;
    public double fdValue;
    public List<YearlyComparison> yearlyData;

    public ComparisonResult(int months, double rdValue, double fdValue, List<YearlyComparison> yearlyData) {
        this.months = months;
        this.rdValue = rdValue;
        this.fdValue = fdValue;
        this.yearlyData = yearlyData;
    }

    public List<YearlyComparison> getYearlyData() {
        return yearlyData;
    }
}
