package com.example;

import com.example.util.DBUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FetchEmployeeServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String empId = request.getParameter("empId");

        String empName = null;
        String dob = null;
        String retirementMonthEnd = null;
        String joinDate = null;
        double pfPay = 0;
        int serviceYears = 0;
        double latestPfPay = 0;
        double projectedAvgPf = 0;
        Map<String, Integer> yearlyProjections = new LinkedHashMap<>();
        Map<String, Double> yearlyOutflow = new LinkedHashMap<>();
        double totalOutflow = 0.0;
        int incrementMonth = 1; // Default to January if not available

        try (Connection con = DBUtil.getConnection()) {

            String query = "SELECT e.emp_name, " +
                    "TO_CHAR(e.birth_date, 'dd-mm-yyyy') AS dob, " +
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
                        
                        // Extract increment month from join_date
                        if (joinDate != null && !joinDate.isEmpty()) {
                            try {
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                                LocalDate joinLocalDate = LocalDate.parse(joinDate, formatter);
                                incrementMonth = joinLocalDate.getMonthValue();
                            } catch (Exception e) {
                                e.printStackTrace();
                                incrementMonth = 1; // Fallback to January if parsing fails
                            }
                        }
                    }
                }
            }

            String latestPfQuery = "SELECT pf_pay FROM vw_dcpy_dpl_payslip WHERE ngs = ? " +
                    "AND (sal_year, sal_month) = ( " +
                    "SELECT sal_year, sal_month FROM ( " +
                    "SELECT sal_year, sal_month FROM vw_dcpy_dpl_payslip " +
                    "WHERE ngs = ? ORDER BY sal_year DESC, sal_month DESC) WHERE ROWNUM = 1)";

            try (PreparedStatement ps = con.prepareStatement(latestPfQuery)) {
                ps.setString(1, empId);
                ps.setString(2, empId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        latestPfPay = rs.getDouble("pf_pay");
                    }
                }
            }

            if (retirementMonthEnd != null && latestPfPay > 0) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                LocalDate retirementDate = LocalDate.parse(retirementMonthEnd, formatter);
                LocalDate currentDate = LocalDate.now();

                projectedAvgPf = calculateProjectedAveragePf(latestPfPay, currentDate, retirementDate, incrementMonth);
                yearlyProjections = getYearWisePfProjection(latestPfPay, currentDate, retirementDate, incrementMonth);
                yearlyOutflow = calculateYearlyOutflow(latestPfPay, yearlyProjections, retirementDate);
                totalOutflow = yearlyOutflow.values().stream().mapToDouble(Double::doubleValue).sum();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Database error occurred", e);
        }

        request.setAttribute("empId", empId);
        request.setAttribute("empName", empName);
        request.setAttribute("dob", dob);
        request.setAttribute("retirementMonthEnd", retirementMonthEnd);
        request.setAttribute("pfPay", pfPay);
        request.setAttribute("joinDate", joinDate);
        request.setAttribute("serviceYears", serviceYears);
        request.setAttribute("latestPfPay", latestPfPay);
        request.setAttribute("projectedAvgPf", projectedAvgPf);
        request.setAttribute("yearlyProjections", yearlyProjections);
        request.setAttribute("yearlyOutflow", yearlyOutflow);
        request.setAttribute("totalOutflow", totalOutflow);
        request.setAttribute("incrementMonth", incrementMonth);

        request.getRequestDispatcher("details.jsp").forward(request, response);
    }

    private static double calculateProjectedAveragePf(double currentPf, LocalDate currentDate, 
            LocalDate retirementDate, int incrementMonth) {
        List<Double> monthlyPfValues = new ArrayList<>();
        double pf = currentPf;
        LocalDate datePointer = currentDate.withDayOfMonth(1); // Start from current month
        
        while (!datePointer.isAfter(retirementDate)) {
            // Special 1.5x hike in January 2030
            if (datePointer.getYear() == 2030 && datePointer.getMonthValue() == 1) {
                pf *= 1.5;
            } 
            // Pre-2030 rules
            else if (datePointer.getYear() < 2030) {
                // Apply 5% DA hike in January
                if (datePointer.getMonthValue() == 1) {
                    pf *= 1.05;
                }
                // Apply 3% increment hike in join month (if not January)
                if (datePointer.getMonthValue() == incrementMonth && incrementMonth != 1) {
                    pf *= 1.03;
                }
            }
            // Post-2030 rules
            else if (datePointer.getYear() > 2030) {
                // Apply 1% DA hike in January
                if (datePointer.getMonthValue() == 1) {
                    pf *= 1.01;
                }
                // Continue 3% increment in join month
                if (datePointer.getMonthValue() == incrementMonth) {
                    pf *= 1.03;
                }
            }
            
            // Store the PF value for this month
            monthlyPfValues.add(pf);
            datePointer = datePointer.plusMonths(1);
        }

        // Get last 60 months (5 years) for average calculation
        int monthsToConsider = Math.min(60, monthlyPfValues.size());
        if (monthsToConsider == 0) return 0;
        
        List<Double> lastMonths = monthlyPfValues.subList(
            Math.max(0, monthlyPfValues.size() - monthsToConsider), 
            monthlyPfValues.size());
        
        return lastMonths.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }

    private static Map<String, Integer> getYearWisePfProjection(double currentPf,
            LocalDate currentDate, LocalDate retirementDate, int incrementMonth) {
        Map<String, Integer> yearWiseProjection = new LinkedHashMap<>();
        double pf = currentPf;
        LocalDate datePointer = currentDate.withDayOfMonth(1);
        int currentYear = datePointer.getYear();
        double decemberPf = pf;
        
        while (!datePointer.isAfter(retirementDate)) {
            // Special 1.5x hike in January 2030
            if (datePointer.getYear() == 2030 && datePointer.getMonthValue() == 1) {
                pf *= 1.5;
            } 
            // Pre-2030 rules
            else if (datePointer.getYear() < 2030) {
                // Apply 5% DA hike in January
                if (datePointer.getMonthValue() == 1) {
                    pf *= 1.05;
                }
                // Apply 3% increment hike in join month (if not January)
                if (datePointer.getMonthValue() == incrementMonth && incrementMonth != 1) {
                    pf *= 1.03;
                }
            }
            // Post-2030 rules
            else if (datePointer.getYear() > 2030) {
                // Apply 1% DA hike in January
                if (datePointer.getMonthValue() == 1) {
                    pf *= 1.01;
                }
                // Continue 3% increment in join month
                if (datePointer.getMonthValue() == incrementMonth) {
                    pf *= 1.03;
                }
            }
            
            // Record December value for each year
            if (datePointer.getMonthValue() == 12) {
                decemberPf = pf;
                yearWiseProjection.put(String.valueOf(datePointer.getYear()), (int) Math.round(decemberPf));
                currentYear++;
                datePointer = datePointer.plusYears(1).withMonth(1);
            } else {
                datePointer = datePointer.plusMonths(1);
            }
        }
        
        // Handle case where retirement is before December
        if (!yearWiseProjection.containsKey(String.valueOf(retirementDate.getYear()))) {
            yearWiseProjection.put(String.valueOf(retirementDate.getYear()), (int) Math.round(pf));
        }
        
        return yearWiseProjection;
    }

    private static Map<String, Double> calculateYearlyOutflow(double initialPfPay, 
            Map<String, Integer> yearlyPfPay, LocalDate retirementDate) {
        Map<String, Double> yearlyOutflow = new LinkedHashMap<>();
        double accumulatedBalance = 0.0;

        for (Map.Entry<String, Integer> entry : yearlyPfPay.entrySet()) {
            int year = Integer.parseInt(entry.getKey());
            int monthlyPf = entry.getValue();
            int monthsInYear = 12;

            if (year == retirementDate.getYear()) {
                monthsInYear = retirementDate.getMonthValue();  // months until retirement month
            }

            double yearlyContribution = monthlyPf * 0.0949 * monthsInYear;
            accumulatedBalance = (accumulatedBalance + yearlyContribution) * 1.085;  // applying 8.5% interest annually
            yearlyOutflow.put(entry.getKey(), (double) Math.round(accumulatedBalance));
        }

        return yearlyOutflow;
    }
}