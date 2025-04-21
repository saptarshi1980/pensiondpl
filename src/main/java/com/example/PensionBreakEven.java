package com.example;

import java.util.Scanner;


public class PensionBreakEven {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Get user input
        System.out.print("Enter the lump sum amount required for pension: ");
        double lumpSum = scanner.nextDouble();

        System.out.print("Enter the monthly pension amount you'll receive: ");
        double monthlyPension = scanner.nextDouble();

        System.out.print("Enter annual FD interest rate (%): ");
        double fdAnnualRate = scanner.nextDouble();

        System.out.print("Enter annual RD interest rate (%): ");
        double rdAnnualRate = scanner.nextDouble();

        System.out.println("\nYear-by-Year Comparison Report:");
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.printf("%5s %15s %15s %15s %15s %15s%n",
                         "Year", "Pension+RD", "FD Only", "Difference", "RD Value", "Status");
        System.out.println("----------------------------------------------------------------------------------------");

        ComparisonResult result = calculateAndCompare(lumpSum, monthlyPension, fdAnnualRate, rdAnnualRate);

        // Display final results
        if (result == null) {
            System.out.println("\nThe break-even point exceeds 100 years. Pension option may not be financially optimal.");
        } else {
            int years = result.months / 12;
            int remainingMonths = result.months % 12;
            
            System.out.println("\nFinal Results:");
            System.out.printf("- Break-even occurs after %d years and %d months%n", years, remainingMonths);
            System.out.printf("- Total Pension+RD value: RS%,.2f%n", result.rdValue);
            System.out.printf("- FD only value: RS%,.2f%n", result.fdValue);
            System.out.printf("- Difference at break-even: RS%,.2f%n", (result.rdValue - result.fdValue));
        }

        scanner.close();
    }

    public static ComparisonResult calculateAndCompare(double lumpSum, double monthlyPension,
                                                     double fdAnnualRate, double rdAnnualRate) {
        double fdMonthlyRate = fdAnnualRate / 100 / 12;
        double rdMonthlyRate = rdAnnualRate / 100 / 12;
        
        double fdValue = lumpSum;
        double rdValue = 0;
        int months = 0;
        int years = 0;
        
        while (rdValue < fdValue) {
            months++;
            
            // Grow the FD with compound interest
            fdValue *= (1 + fdMonthlyRate);
            
            // Add pension payment to RD and grow it
            rdValue += monthlyPension;
            rdValue *= (1 + rdMonthlyRate);
            
            // Print yearly summary
            if (months % 12 == 0) {
                years++;
                double difference = rdValue - fdValue;
                String status = difference < 0 ? "FD Better" : "Pension+RD Better";
                System.out.printf("%5d %,15.2f %,15.2f %,15.2f %,15.2f %15s%n",
                                years, rdValue, fdValue, difference, rdValue, status);
            }
            
            // Safety check
            if (months > 1200) {  // 100 years
                return null;
            }
        }
        
        // Print final month if it doesn't fall on a year boundary
        if (months % 12 != 0) {
            double difference = rdValue - fdValue;
            System.out.printf("%5.1f %,15.2f %,15.2f %,15.2f %,15.2f %15s%n",
                            months/12.0, rdValue, fdValue, difference, rdValue, "BREAK-EVEN");
        }
        
        return new ComparisonResult(months, rdValue, fdValue);
    }

    static class ComparisonResult {
        int months;
        double rdValue;
        double fdValue;

        public ComparisonResult(int months, double rdValue, double fdValue) {
            this.months = months;
            this.rdValue = rdValue;
            this.fdValue = fdValue;
        }
    }
}