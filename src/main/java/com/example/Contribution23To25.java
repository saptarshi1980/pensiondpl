package com.example;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.FetchEmployeeServlet.PayComponents;

public class Contribution23To25 {
	
	
	public static class PayComponents {
		double basic;
		double da;

		PayComponents(double basic, double da) {
			this.basic = basic;
			this.da = da;
		}

		public double getPfPay() {
			return basic + da;
		}

		public double getBasic() {
			return basic;
		}

		public void setBasic(double basic) {
			this.basic = basic;
		}

		public double getDa() {
			return da;
		}

		public void setDa(double da) {
			this.da = da;
		}

		public PayComponents(PayComponents other) {
			this.basic = other.basic;
			this.da = other.da;
		}
	}


	private static Map<String, Double> calculateTwoYearMonthlyOutflow(PayComponents initialPay, PrintWriter logWriter, 
	        String empId, String empName, int incrementMonth, double openingPfBalance) {
	    
	    Map<String, Double> monthlyOutflows = new LinkedHashMap<>();
	    double balance = openingPfBalance;
	    double monthlyInterestRate = 0.0825 / 12;
	    double totalOutflow = 0;

	    // Log header information
	    logWriter.println("==============================================================================================================");
	    logWriter.println("                    Monthly PF Outflow Calculation (April 2023 - April 2025)");
	    logWriter.println("==============================================================================================================");
	    logWriter.println("Employee ID: " + empId);
	    logWriter.println("Employee Name: " + empName);
	    logWriter.printf("Opening PF Balance (as of April 2023): %,15.2f%n", balance);
	    logWriter.println("Formula used as follows");
	    logWriter.println("========================================================================================================================================================================================================================");
	    logWriter.println("If PF Pay exceeds Rs 15000/-, Contribution Outflow = (8.33% of PF Pay plus 1.16% of (PF Pay minus Rs 15,000) minus Rs 1,250). If PF Pay is Rs 15,000 or less, the contribution is zero.");
	    logWriter.println("========================================================================================================================================================================================================================");
	    logWriter.println("---------------------------------------------------------------------------------------------------------------------------------------------");
	    logWriter.printf("%-14s %14s %15s %15s %18s %18s %15s %18s%n", "Year", "Month", "Basic", "DA",
	            "Opening Bal", "Contribution", "Interest", "Closing Bal");
	    logWriter.println("----------------------------------------------------------------------------------------------------------------------------------------");

	    PayComponents monthlyPay = new PayComponents(initialPay.getBasic(), initialPay.getDa());
	    
	    LocalDate startDate = LocalDate.of(2023, 4, 1);
	    LocalDate endDate = LocalDate.of(2025, 4, 30);
	    LocalDate currentDate = startDate;

	    while (!currentDate.isAfter(endDate)) {
	        int month = currentDate.getMonthValue();
	        int year = currentDate.getYear();
	        String monthName = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
	                                         "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" }[month - 1];

	        // Apply salary updates
	        if (month == incrementMonth) {
	            monthlyPay.setBasic(monthlyPay.getBasic() * 1.03); // 3% annual increment
	        }

	        // Apply DA adjustments in January each year
	        if (month == 1) {
	            if (year == 2024) {
	                monthlyPay.setDa(monthlyPay.getDa() + monthlyPay.getBasic() * 0.05); // 5% DA hike
	            } else if (year == 2025) {
	                monthlyPay.setDa(monthlyPay.getDa() + monthlyPay.getBasic() * 0.05); // 5% DA hike
	            }
	        }

	        // Calculate PF contribution
	        double pfPay = monthlyPay.getPfPay();
	        double monthlyContribution = (pfPay > 15000) ? 
	            (pfPay * 0.0833) + ((pfPay - 15000) * 0.0116) - 1250 : 0;
	        double netMonthlyContribution = monthlyContribution; // No admin fee in this version

	        // Process month
	        double openingBalance = balance;
	        double interest = balance * monthlyInterestRate;
	        balance += interest + netMonthlyContribution;

	        // Log monthly details
	        logWriter.printf("%-14s %14s %,15.2f %,15.2f %,18.2f %,18.2f %,15.2f %,18.2f%n", 
	                year, monthName, monthlyPay.getBasic(), monthlyPay.getDa(), openingBalance,
	                netMonthlyContribution, interest, balance);

	        // Store monthly outflow
	        String monthYearKey = monthName + " " + year;
	        monthlyOutflows.put(monthYearKey, netMonthlyContribution);
	        totalOutflow += netMonthlyContribution;

	        currentDate = currentDate.plusMonths(1);
	    }

	    // Add financial year summaries
	    double fy2023_24 = monthlyOutflows.entrySet().stream()
	            .filter(e -> e.getKey().endsWith("2023") || e.getKey().startsWith("Apr 2024") || e.getKey().startsWith("May 2024") || e.getKey().startsWith("Jun 2024"))
	            .mapToDouble(Map.Entry::getValue)
	            .sum();
	    
	    double fy2024_25 = totalOutflow - fy2023_24;

	    // Final report
	    logWriter.println("----------------------------------------------------------------------------------------------------------------------------------------");
	    logWriter.printf("%-14s %14s %15s %15s %18s %18s %15s %,18.2f%n", 
	            "", "TOTAL", "", "", "", totalOutflow, "", balance);
	    logWriter.println("==============================================================================================================");
	    logWriter.println("SUMMARY OF 2-YEAR PF OUTFLOW CALCULATION");
	    logWriter.println("==============================================================================================================");
	    logWriter.printf("FY 2023-24 Total Outflow: %,15.2f%n", fy2023_24);
	    logWriter.printf("FY 2024-25 Total Outflow: %,15.2f%n", fy2024_25);
	    logWriter.printf("Combined Total Outflow: %,15.2f%n", totalOutflow);
	    logWriter.printf("Final PF Balance (April 2025): %,15.2f%n", balance);
	    logWriter.println("==============================================================================================================");

	    return monthlyOutflows;
	}
}
