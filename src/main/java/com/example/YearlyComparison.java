package com.example;

public class YearlyComparison {
    public String yearLabel;
    public double rdValue;
    public double fdValue;
    public double difference;
    public String status;

    public YearlyComparison(double year, double rdValue, double fdValue, double difference, String status) {
        this.yearLabel = String.format("%.1f", year);
        this.rdValue = rdValue;
        this.fdValue = fdValue;
        this.difference = difference;
        this.status = status;
    }

    public String getYearLabel() {
        return yearLabel;
    }

    public double getRdValue() {
        return rdValue;
    }

    public double getFdValue() {
        return fdValue;
    }

    public double getDifference() {
        return difference;
    }

    public String getStatus() {
        return status;
    }
}
