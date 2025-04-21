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
        int incrementMonth = 1;

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
                        System.out.println("Join date from DB: '" + joinDate + "'");

                        if (joinDate != null && !joinDate.isEmpty()) {
                            try {
                                LocalDate joinLocalDate = LocalDate.parse(joinDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                                incrementMonth = joinLocalDate.getMonthValue();
                            } catch (Exception e) {
                                e.printStackTrace();
                                incrementMonth = 1;
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
                LocalDate retirementDate = LocalDate.parse(retirementMonthEnd, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
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
        List<Double> monthlyPf = new ArrayList<>();
        double pf = currentPf;
        LocalDate datePointer = currentDate.withDayOfMonth(1);

        while (!datePointer.isAfter(retirementDate)) {
            pf = applyPfHikeRules(pf, datePointer, incrementMonth);
            monthlyPf.add(pf);
            datePointer = datePointer.plusMonths(1);
        }

        int months = Math.min(60, monthlyPf.size());
        return months == 0 ? 0 :
                monthlyPf.subList(monthlyPf.size() - months, monthlyPf.size())
                         .stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static Map<String, Integer> getYearWisePfProjection(double currentPf, LocalDate currentDate, 
                                                                LocalDate retirementDate, int incrementMonth) {
        Map<String, Integer> projections = new LinkedHashMap<>();
        double pf = currentPf;
        LocalDate datePointer = currentDate.withDayOfMonth(1);

        while (!datePointer.isAfter(retirementDate)) {
            pf = applyPfHikeRules(pf, datePointer, incrementMonth);

            if (datePointer.getMonthValue() == 12) {
                projections.put(String.valueOf(datePointer.getYear()), (int) Math.round(pf));
                datePointer = datePointer.plusYears(1).withMonth(1);
            } else {
                datePointer = datePointer.plusMonths(1);
            }
        }
        return projections;
    }

    private static Map<String, Double> calculateYearlyOutflow(double basePf, Map<String, Integer> projections, 
                                                              LocalDate retirementDate) {
        Map<String, Double> outflows = new LinkedHashMap<>();
        double balance = 0;

        for (Map.Entry<String, Integer> entry : projections.entrySet()) {
            String year = entry.getKey();
            int pfValue = entry.getValue();

            int months = 12;
            if (Integer.parseInt(year) == retirementDate.getYear()) {
                months = retirementDate.getMonthValue();
            }

            double yearlyContribution = (months * (pfValue-15000) * 0.0949)+(months * 15000 * 0.0833);
            /* Manoj da told the above logic, i want to compare the difference between two calculation */
            double yearlyContributionold = (months * pfValue * 0.0949);
             
            System.out.println("Yearly Contribution-"+yearlyContribution+"*** Yearly Contribution old-"+yearlyContributionold);
            balance += yearlyContribution;
            balance *= 1.0825; // Add 8.25% interest
            outflows.put(year, Math.round(balance * 100.0) / 100.0);
        }

        return outflows;
    }

    private static double applyPfHikeRules(double pf, LocalDate date, int incrementMonth) {
        int year = date.getYear();
        int month = date.getMonthValue();

        if (year == 2030 && month == 1) {
            return pf * 1.5;
        }
        if (year == 2040 && month == 1) {
            return pf * 1.25;
        }

        if (year < 2030) {
        	if (month == incrementMonth) pf *= 1.03;
        	if (month == 1) pf *= 1.05;
            
        } else if (year >= 2031 && year <= 2040) {
        	if (month == incrementMonth) pf *= 1.03;
        	if (month == 1) pf *= 1.01;
            
        } else if (year > 2040) {
        	if (month == incrementMonth) pf *= 1.03;
        	if (month == 1) pf *= 1.01;
            
        }
        return pf;
    }
}
