package com.example;

public class RowData {
    public String year;
    public double pensionValue;
    public double fdValue;
    public double difference;
    public String status;

    public RowData(String year, double pensionValue, double fdValue, double difference, String status) {
        this.year = year;
        this.pensionValue = pensionValue;
        this.fdValue = fdValue;
        this.difference = difference;
        this.status = status;
    }
    
    public String getYear() {
        return year;
    }

    public double getPensionValue() {
        return pensionValue;
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
