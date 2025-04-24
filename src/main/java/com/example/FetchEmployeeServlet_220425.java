package com.example;

import com.example.util.DBUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FetchEmployeeServlet_220425 extends HttpServlet {

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
    
    // Add this static field to store the projections during calculateYearlyOutflow execution
    private static Map<String, PayComponents> lastCalculatedProjections = new LinkedHashMap<>();

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Create a StringWriter to capture the console output
        StringWriter stringWriter = new StringWriter();
        PrintWriter logWriter = new PrintWriter(stringWriter);

        String empId = request.getParameter("empId");
        boolean downloadReport = "true".equals(request.getParameter("downloadReport"));

        String empName = null, dob = null, retirementMonthEnd = null, joinDate = null;
        double pfPay = 0, projectedAvgPf = 0, totalOutflow = 0;
        int serviceYears = 0, incrementMonth = 1;
        PayComponents payComponents = new PayComponents(0, 0);
        Map<String, PayComponents> yearlyProjections = new LinkedHashMap<>();
        Map<String, Double> yearlyOutflow = new LinkedHashMap<>();

        try (Connection con = DBUtil.getConnection()) {

            // Fetch employee & salary info
            String query = "SELECT e.emp_name, TO_CHAR(e.birth_date, 'dd-mm-yyyy') AS dob, " +
                    "TO_CHAR(LAST_DAY(ADD_MONTHS(e.birth_date, 58 * 12)), 'dd-mm-yyyy') AS retirement_month_end, " +
                    "p.pf_pay, p.join_date, " +
                    "FLOOR(MONTHS_BETWEEN(TO_DATE('31-08-2014', 'dd-mm-yyyy'), TO_DATE(p.join_date, 'dd-mm-yyyy')) / 12 + 0.5) AS service_years " +
                    "FROM app_vw_emp e JOIN vw_dcpy_dpl_payslip p ON e.emp_id = p.ngs " +
                    "WHERE p.ngs = ? AND p.sal_month = 8 AND p.sal_year = 2014";

            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.setString(1, empId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        empName = rs.getString("emp_name");
                        dob = rs.getString("dob");
                        retirementMonthEnd = rs.getString("retirement_month_end");
                        pfPay = rs.getDouble("pf_pay");
                        joinDate = rs.getString("join_date");
                        serviceYears = rs.getInt("service_years");

                        if (joinDate != null && !joinDate.isEmpty()) {
                            LocalDate joinLocalDate = LocalDate.parse(joinDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            incrementMonth = joinLocalDate.getMonthValue();
                            //logWriter.println("Increment month fetched from data: " + incrementMonth);
                        }
                    }
                }
            }

            // Fetch latest PF details
            String latestPfQuery = "SELECT pf_pay, basic_salary, dearness_allowance FROM vw_dcpy_dpl_payslip WHERE ngs = ? " +
                    "AND (sal_year, sal_month) = (SELECT sal_year, sal_month FROM ( " +
                    "SELECT sal_year, sal_month FROM vw_dcpy_dpl_payslip " +
                    "WHERE ngs = ? ORDER BY sal_year DESC, sal_month DESC) WHERE ROWNUM = 1)";

            try (PreparedStatement ps = con.prepareStatement(latestPfQuery)) {
                ps.setString(1, empId);
                ps.setString(2, empId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        payComponents.basic = rs.getDouble("basic_salary");
                        payComponents.da = rs.getDouble("dearness_allowance");
                    }
                }
            }

            // Add report header with employee information
            logWriter.println("==============================================================================================================");
            logWriter.println("                                       PF PROJECTION REPORT");
            logWriter.println("==============================================================================================================");
            logWriter.println("Employee ID: " + empId);
            logWriter.println("Employee Name: " + empName);
            logWriter.println("Date of Birth: " + dob);
            logWriter.println("Retirement Date: " + retirementMonthEnd);
            logWriter.println("Service Years before Sept 2014: " + serviceYears);
            logWriter.println("==============================================================================================================");

            if (retirementMonthEnd != null && payComponents.getPfPay() > 0) {
                LocalDate retirementDate = LocalDate.parse(retirementMonthEnd, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                LocalDate currentDate = LocalDate.now();
                
                // First, calculate yearly outflow - this will also populate lastCalculatedProjections
                yearlyOutflow = calculateYearlyOutflow(payComponents, retirementDate, logWriter, empId, empName, incrementMonth);
                
                // Then extract projections from the calculateYearlyOutflow method's internal calculations
                yearlyProjections = extractYearlyProjections();
                
                // Calculate projected average PF
                projectedAvgPf = calculateProjectedAveragePf(currentDate, retirementDate, incrementMonth, new PayComponents(payComponents.basic, payComponents.da), logWriter);
                
                totalOutflow = yearlyOutflow.values().stream().mapToDouble(Double::doubleValue).sum();
            }

        } catch (Exception e) {
            e.printStackTrace(logWriter); // Write stack trace to our log
            throw new ServletException("Database error occurred", e);
        }

        // Flush the writer to ensure all content is captured
        logWriter.flush();
        
        // If download parameter is true, send the log as a downloadable file
        if (downloadReport) {
            // Set the content type and header for the response
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=\"PF_Projection_" + empId + ".txt\"");
            
            // Write the captured output to the response
            try (PrintWriter out = response.getWriter()) {
                out.write(stringWriter.toString());
            }
        } else {
            // Normal flow - show the JSP page
            request.setAttribute("empId", empId);
            request.setAttribute("empName", empName);
            request.setAttribute("dob", dob);
            request.setAttribute("retirementMonthEnd", retirementMonthEnd);
            request.setAttribute("pfPay", pfPay);
            request.setAttribute("joinDate", joinDate);
            request.setAttribute("serviceYears", serviceYears);
            request.setAttribute("latestPfPay", payComponents.getPfPay());
            request.setAttribute("projectedAvgPf", projectedAvgPf);
            request.setAttribute("yearlyProjections", yearlyProjections);
            request.setAttribute("yearlyOutflow", yearlyOutflow);
            request.setAttribute("totalOutflow", totalOutflow);
            request.setAttribute("incrementMonth", incrementMonth);
            request.setAttribute("consoleOutput", stringWriter.toString());

            request.getRequestDispatcher("details.jsp").forward(request, response);
        }
    }

	/*
	 * private static double calculateProjectedAveragePf(LocalDate currentDate,
	 * LocalDate retirementDate, int incrementMonth, PayComponents pay, PrintWriter
	 * logWriter) { List<Double> monthlyPf = new ArrayList<>(); LocalDate
	 * datePointer = currentDate.withDayOfMonth(1);
	 * 
	 * while (!datePointer.isAfter(retirementDate)) { double pf =
	 * applyPfHikeRules(pay, datePointer, incrementMonth, logWriter);
	 * monthlyPf.add(pf); datePointer = datePointer.plusMonths(1); }
	 * 
	 * int months = Math.min(60, monthlyPf.size()); return months == 0 ? 0 :
	 * monthlyPf.subList(monthlyPf.size() - months, monthlyPf.size()).stream()
	 * .mapToDouble(Double::doubleValue).average().orElse(0); }
	 */
    
    
    private static double calculateProjectedAveragePf(LocalDate currentDate, LocalDate retirementDate, int incrementMonth, PayComponents initialPay, PrintWriter logWriter) {
        List<Double> monthlyPf = new ArrayList<>();
        LocalDate datePointer = currentDate.withDayOfMonth(1);
        
        // Create a new copy to avoid modifying the original
        PayComponents currentPay = new PayComponents(initialPay);

        while (!datePointer.isAfter(retirementDate)) {
            // Apply the rules for this month
            double pfPay = applyPfHikeRules(currentPay, datePointer, incrementMonth, logWriter);
            
            // Store the PF pay for this month
            monthlyPf.add(pfPay);
            
            // Move to next month
            datePointer = datePointer.plusMonths(1);
        }

        // Calculate average of last 60 months or all months if less than 60
        int months = Math.min(60, monthlyPf.size());
        if (months == 0) return 0;
        
        double sum = 0;
        for (int i = monthlyPf.size() - months; i < monthlyPf.size(); i++) {
            sum += monthlyPf.get(i);
        }
        
        return sum / months;
    }
    
    // Method to access the projections extracted during the last calculateYearlyOutflow run
    private static Map<String, PayComponents> extractYearlyProjections() {
        // Return a copy of the stored projections to prevent modification
        Map<String, PayComponents> result = new LinkedHashMap<>();
        for (Map.Entry<String, PayComponents> entry : lastCalculatedProjections.entrySet()) {
            result.put(entry.getKey(), new PayComponents(entry.getValue()));
        }
        return result;
    }

    private static Map<String, Double> calculateYearlyOutflow(PayComponents initialPay, LocalDate retirementDate, 
            PrintWriter logWriter, String empId, String empName, int incrementMonth) {
        
        // Clear previous projections
        lastCalculatedProjections.clear();
        
        Map<String, Double> yearlyOutflows = new LinkedHashMap<>();
        double balance = 0;
        double monthlyInterestRate = 0.0825 / 12;
        double finalRetirementBalance = 0;

        logWriter.println(
                "==============================================================================================================");
        logWriter.println("                          Monthly PF Outflow Calculation with Salary Components");
        logWriter.println(
                "==============================================================================================================");
        //logWriter.println("Employee ID: " + empId);
        //logWriter.println("Employee Name: " + empName);
        logWriter.println("Formula used as follows");
        logWriter.println(
                "========================================================================================================================================================================================================================");
        logWriter.println(
                "If PF Pay exceeds Rs 15,000, Contribution Outflow = (8.33% of PF Pay plus 1.16% of (PF Pay minus Rs 15,000) minus Rs 1,250). If PF Pay is Rs 15,000 or less, the contribution is zero.. Contribution accumulated year on year");
        logWriter.println(
                "========================================================================================================================================================================================================================");
        logWriter.println(
                "---------------------------------------------------------------------------------------------------------------------------------------------");
        logWriter.printf("%-14s %14s %15s %15s %18s %18s %15s %18s%n", "Financial Year", "Month", "Basic", "DA",
                "Opening Bal", "Contribution", "Interest", "Closing Bal");

        logWriter.println(
                "----------------------------------------------------------------------------------------------------------------------------------------");

        // Start date and current pay components
        LocalDate startDate = LocalDate.of(2025, 4, 1); // April 2025
        LocalDate currentDate = startDate;

        // Initialize with the first available data
        PayComponents monthlyPay = new PayComponents(initialPay.getBasic(), initialPay.getDa());

        while (!currentDate.isAfter(retirementDate)) {
            int month = currentDate.getMonthValue();
            int year = currentDate.getYear();
            boolean isLastMonth = month == 3; // March is last month of financial year
            String monthName = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct",
                    "Nov", "Dec" }[month - 1];

            // Apply monthly salary updates

            // Apply increment in the employee's joining month
            if (month == incrementMonth) {
                monthlyPay.setBasic(monthlyPay.getBasic() * 1.03);
            }

            // Apply January adjustments
            if (month == 1) {
                // Special case handling for 2030
                if (year == 2030) {
                    monthlyPay.setBasic(monthlyPay.getBasic() * 1.87 * 1.03 * 1.03 * 1.03);
                    monthlyPay.setDa(monthlyPay.getBasic() * 0.02);
                } 
                // Special case handling for 2040
                else if (year == 2040) {
                    monthlyPay.setBasic(monthlyPay.getBasic() * 1.4 * 1.03 * 1.03 * 1.03);
                    monthlyPay.setDa(monthlyPay.getBasic() * 0.01);
                }
                // Regular DA adjustments
                else {
                    if (year < 2030) {
                        monthlyPay.setDa(monthlyPay.getDa() + monthlyPay.getBasic() * 0.05);
                    } else if (year >= 2030 && year <= 2040) {
                        monthlyPay.setDa(monthlyPay.getDa() + monthlyPay.getBasic() * 0.02);
                    } else if (year > 2040) {
                        monthlyPay.setDa(monthlyPay.getDa() + monthlyPay.getBasic() * 0.005);
                    }
                }
            }

            // Calculate contribution based on current pay
            double pfPay = monthlyPay.getPfPay();
            double monthlyContribution = (pfPay > 15000) ? (pfPay * 0.0833) + ((pfPay - 15000) * 0.0116) - 1250 : 0;
            double adminFeePerMonth = 0;
            double netMonthlyContribution = monthlyContribution - adminFeePerMonth;

            // Process this month
            double openingBalance = balance;
            double interest = balance * monthlyInterestRate;
            balance += interest;

            if (!isLastMonth) {
                balance += netMonthlyContribution;
            }

            // Format the financial year correctly
            String displayYear = (month >= 4) ? String.valueOf(year) : String.valueOf(year - 1);

            logWriter.printf("%-14s %14s %,15.2f %,15.2f %,18.2f %,18.2f %,15.2f %,18.2f%n", displayYear+"-"+(Integer.parseInt(displayYear)+1), monthName,
                    monthlyPay.getBasic(), monthlyPay.getDa(), openingBalance,
                    (!isLastMonth) ? netMonthlyContribution : 0.0, interest, balance);

            // Special handling for March (end of fiscal year)
            if (isLastMonth) {
                openingBalance = balance;
                balance += netMonthlyContribution;
                logWriter.printf("%-14s %14s %,15.2f %,15.2f %,18.2f %,18.2f %,15.2f %,18.2f%n", displayYear+"-"+(Integer.parseInt(displayYear)+1),
                        monthName + "*", monthlyPay.getBasic(), monthlyPay.getDa(), openingBalance,
                        netMonthlyContribution, 0.0, balance);

                // Store the year's total
                String financialYear = String.valueOf(year - 1);
                yearlyOutflows.put(financialYear, Math.round(balance * 100.0) / 100.0);

                logWriter.println(
                        "--------------------------------------------------------------------------------------------------------------------------------------------------------");
                logWriter.printf("%-14s %14s %15s %15s %18s %18s %,15.2f %18s%n", "Financial Year", financialYear+"-"+(Integer.parseInt(financialYear)+1), "",
                        "", "", "Total:", yearlyOutflows.get(financialYear), "");

                logWriter.println(
                        "--------------------------------------------------------------------------------------------------------------------------------------------------------");
            }

            // Store December values for each year for projections
            if (month == 12) {
                lastCalculatedProjections.put(String.valueOf(year), new PayComponents(monthlyPay));
                //logWriter.println("Storing projection for December " + year + ": Basic=" + monthlyPay.getBasic() + ", DA=" + monthlyPay.getDa());
            }

            // Check if this is the last month before or equal to retirement
            if (currentDate.getYear() == retirementDate.getYear() && currentDate.getMonthValue() == retirementDate.getMonthValue()) {
                // This is the retirement month - store final balance
                finalRetirementBalance = balance;
                
                // Add a special entry for retirement month
                yearlyOutflows.put("retirement", Math.round(balance * 100.0) / 100.0);
                
                // Store retirement month projection if not December
                if (month != 12) {
                    lastCalculatedProjections.put(String.valueOf(year), new PayComponents(monthlyPay));
					/*
					 * logWriter.println("Storing projection for retirement month " + monthName +
					 * " " + year + ": Basic=" + monthlyPay.getBasic() + ", DA=" +
					 * monthlyPay.getDa());
					 */
                }
            }

            // Move to next month
            currentDate = currentDate.plusMonths(1);

            // Hard stop at retirement date
            if (currentDate.isAfter(retirementDate)) {
                break;
            }
        }
        
        // Add total row at the end of the report
        logWriter.println(
                "==============================================================================================================");
        logWriter.println("SUMMARY OF PF OUTFLOW CALCULATION");
        logWriter.println(
                "==============================================================================================================");
        logWriter.printf("Final PF Balance at Retirement (November 2035): %,15.2f%n", finalRetirementBalance);
        logWriter.println(
                "==============================================================================================================");

        // Log all extracted projections
        logWriter.println("\nExtracted year-wise projections:");
        for (Map.Entry<String, PayComponents> entry : lastCalculatedProjections.entrySet()) {
            logWriter.println("Year " + entry.getKey() + ": Basic=" + entry.getValue().getBasic() + 
                             ", DA=" + entry.getValue().getDa());
        }

        return yearlyOutflows;
    }
    
    private static double applyPfHikeRules(PayComponents pay, LocalDate date, int incrementMonth, PrintWriter logWriter) {
        int year = date.getYear(), month = date.getMonthValue();

        // Apply regular yearly increment based on increment month for all years
        if (month == incrementMonth) {
            pay.basic *= 1.03;
            logWriter.println("Applying regular 3% increment in " + date.getMonth() + " " + year + " (Basic after: " + pay.basic + ")");
        }

        // DA adjustments in January for different year ranges
        if (month == 1) {
            // Special case handling for 2030
            if (year == 2030) {
                logWriter.println("Applying special 2030 multiplier (1.87) and three 3% hikes in January " + year + 
                                " (Basic before: " + pay.basic + ")");
                // Apply the special 1.87 multiplier
                pay.basic *= 1.87;
                // Apply three 3% hikes
                pay.basic = pay.basic * 1.03 * 1.03 * 1.03;
                pay.da = pay.basic * 0.02;
                logWriter.println("Basic after special 2030 adjustment: " + pay.basic);
            } 
            // Special case handling for 2040
            else if (year == 2040) {
                logWriter.println("Applying special 2040 multiplier (1.4) and three 3% hikes in January " + year + 
                                " (Basic before: " + pay.basic + ")");
                // Apply the special 1.4 multiplier
                pay.basic *= 1.4;
                // Apply three 3% hikes
                pay.basic = pay.basic * 1.03 * 1.03 * 1.03;
                pay.da = pay.basic * 0.01;
                logWriter.println("Basic after special 2040 adjustment: " + pay.basic);
            }
            // Regular DA adjustments in January
            else {
                double daIncrease = 0;
                if (year < 2030) {
                    daIncrease = pay.basic * 0.05;
                } else if (year >= 2030 && year <= 2040) {
                    daIncrease = pay.basic * 0.02;
                } else if (year > 2040) {
                    daIncrease = pay.basic * 0.005;
                }
                
                if (daIncrease > 0) {
                    logWriter.println("Applying DA adjustment in January " + year + 
                                    " (DA before: " + pay.da + ", Increase: " + daIncrease + ")");
                    pay.da += daIncrease;
                    logWriter.println("DA after adjustment: " + pay.da);
                }
            }
        }
        
        return pay.getPfPay();
    }
}

