package com.example;

import com.example.PFMonthlyCalculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.example.util.DBUtil;

public class PFCalculator {
    private static final double ANNUAL_INTEREST_RATE = 0.0825; // 8.25%
    private static final double MONTHLY_INTEREST_RATE = ANNUAL_INTEREST_RATE / 12;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

	/*
	 * public void main(String[] args) { //String employeeCode = "13550"; //double
	 * openingBalanceApril2023 = 1172000.00;
	 * 
	 * try (Connection con = DBUtil.getConnection()) { // Step 1: Fetch payslip data
	 * from database List<PaySlip> paySlips = fetchPaySlips(con, employeeCode);
	 * 
	 * // Step 2: Calculate PF projections Map<String, PFMonthlyCalculation> results
	 * = calculatePFProjection(paySlips, openingBalanceApril2023);
	 * 
	 * // Step 3: Print results printResults(results);
	 * 
	 * } catch (SQLException e) { e.printStackTrace(); } }
	 */
    // Database fetch method
    public List<PaySlip> fetchPaySlips(Connection conn, String employeeCode) throws SQLException {
        List<PaySlip> paySlips = new ArrayList<>();
        String sql = "SELECT nam,sal_month, sal_year, basic_salary, dearness_allowance "
                   + "FROM vw_dcpy_dpl_payslip "
                   + "WHERE ngs = ? AND ((sal_year = 2023 AND sal_month >= 4) "
                   + "OR (sal_year = 2024) OR (sal_year = 2025 AND sal_month <= 4)) "
                   + "ORDER BY sal_year, sal_month";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, employeeCode);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                paySlips.add(new PaySlip(rs.getString("nam"),
                    rs.getInt("sal_month"),
                    rs.getInt("sal_year"),
                    rs.getDouble("basic_salary"),
                    rs.getDouble("dearness_allowance")
                ));
            }
        }
        return paySlips;
    }

    // PF calculation logic
    public Map<String, PFMonthlyCalculation> calculatePFProjection(
            List<PaySlip> paySlips, double initialBalance) {
        
        Map<String, PFMonthlyCalculation> results = new LinkedHashMap<>();
        double balanceWithoutInterest = initialBalance;
        double accruedInterest = 0.0;
        
        for (PaySlip slip : paySlips) {
            int month = slip.getMonth();
            int year = slip.getYear();
            YearMonth yearMonth = YearMonth.of(year, month);
            String monthKey = yearMonth.format(MONTH_FORMAT);
            
            double pfPay = slip.getBasicSalary() + slip.getDearnessAllowance();
            double contribution = calculatePFContribution(pfPay);
            
            // Calculate interest accrued this month (but not added to balance yet)
            double monthlyInterest = round(balanceWithoutInterest * MONTHLY_INTEREST_RATE);
            accruedInterest += monthlyInterest;
            
            // Opening balance for this month (including previously accrued interest)
            double openingBalance = balanceWithoutInterest;
            
            // For end of financial year (March), add all accrued interest to the balance
            if (month == 3) {
                balanceWithoutInterest += accruedInterest;
                accruedInterest = 0.0;
            }
            
            // Add this month's contribution
            balanceWithoutInterest += contribution;
            
            // Total balance if interest were to be credited now (for display only)
            double closingBalanceWithInterest = round(balanceWithoutInterest + accruedInterest);
            // Actual closing balance (without unaccredited interest)
            double closingBalance = round(balanceWithoutInterest);
            
            results.put(monthKey, new PFMonthlyCalculation(
                slip.getBasicSalary(),
                slip.getDearnessAllowance(),
                pfPay,
                contribution,
                monthlyInterest,
                openingBalance,
                closingBalance,
                accruedInterest,
                closingBalanceWithInterest
            ));
        }
        return results;
    }

    private static double calculatePFContribution(double pfPay) {
        if (pfPay <= 15000) return 0;
        double contribution = (pfPay * 0.0833) + ((pfPay - 15000) * 0.0116) - 1250;
        return round(Math.max(contribution, 0)); // Ensure not negative
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static void printResults(Map<String, PFMonthlyCalculation> results) {
        System.out.println("PF Projection Report (Apr 2023 - Apr 2025)");
        System.out.println("============================================================================================================================");
        System.out.printf("%-10s %10s %10s %10s %12s %12s %15s %15s %15s%n",
                "Month", "Basic", "DA", "PF Pay", "Contribution", "Interest", "Opening Bal", "Accrued Int", "Closing Bal");
        
        results.forEach((month, calc) -> {
            System.out.printf("%-10s %,10.2f %,10.2f %,10.2f %,12.2f %,12.2f %,15.2f %,15.2f %,15.2f%n",
                    month,
                    calc.getBasicSalary(),
                    calc.getDearnessAllowance(),
                    calc.getPfPay(),
                    calc.getContribution(),
                    calc.getMonthlyInterest(),
                    calc.getOpeningBalance(),
                    calc.getAccruedInterest(),
                    calc.getClosingBalance());
        });
        
        // Print summary
        double totalContributions = results.values().stream()
                .mapToDouble(PFMonthlyCalculation::getContribution)
                .sum();
        double totalInterest = results.values().stream()
                .mapToDouble(PFMonthlyCalculation::getMonthlyInterest)
                .sum();
        
        System.out.println("============================================================================================================================");
        System.out.printf("%-10s %60s %,15.2f%n", "Total", "Contributions:", totalContributions);
        System.out.printf("%-10s %60s %,15.2f%n", "Total", "Interest Accrued:", totalInterest);
        
        PFMonthlyCalculation lastCalc = results.values().stream()
                .reduce((first, second) -> second)
                .orElse(null);
        
        if (lastCalc != null) {
            System.out.printf("%-10s %60s %,15.2f%n", "Final", "PF Balance:", lastCalc.getClosingBalanceWithInterest());
        }
    }

    // Data classes
   public static class PaySlip {
        private final String empName;
	    private final int month;
        private final int year;
        private final double basicSalary;
        private final double dearnessAllowance;

        public PaySlip(String empName,int month, int year, double basicSalary, double dearnessAllowance) {
            this.empName = empName;
        	this.month = month;
            this.year = year;
            this.basicSalary = basicSalary;
            this.dearnessAllowance = dearnessAllowance;
        }

        public int getMonth() {
            return month;
        }

        public int getYear() {
            return year;
        }

        public double getBasicSalary() {
            return basicSalary;
        }

        public double getDearnessAllowance() {
            return dearnessAllowance;
        }

		public String getEmpName() {
			return empName;
		}
    }


}