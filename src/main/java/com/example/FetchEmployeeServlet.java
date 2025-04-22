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

    static class PayComponents {
        double basic;
        double da;
        PayComponents(double basic, double da) {
            this.basic = basic;
            this.da = da;
        }

        double getPfPay() {
            return basic + da;
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String empId = request.getParameter("empId");

        String empName = null, dob = null, retirementMonthEnd = null, joinDate = null;
        double pfPay = 0, projectedAvgPf = 0, totalOutflow = 0;
        int serviceYears = 0, incrementMonth = 1;
        PayComponents payComponents = new PayComponents(0, 0);
        Map<String, Integer> yearlyProjections = new LinkedHashMap<>();
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

            if (retirementMonthEnd != null && payComponents.getPfPay() > 0) {
                LocalDate retirementDate = LocalDate.parse(retirementMonthEnd, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                LocalDate currentDate = LocalDate.now();

                projectedAvgPf = calculateProjectedAveragePf(currentDate, retirementDate, incrementMonth, new PayComponents(payComponents.basic, payComponents.da));
                yearlyProjections = getYearWisePfProjection(currentDate, retirementDate, incrementMonth, new PayComponents(payComponents.basic, payComponents.da));
                yearlyOutflow = calculateYearlyOutflow(yearlyProjections, retirementDate);
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
        request.setAttribute("latestPfPay", payComponents.getPfPay());
        request.setAttribute("projectedAvgPf", projectedAvgPf);
        request.setAttribute("yearlyProjections", yearlyProjections);
        request.setAttribute("yearlyOutflow", yearlyOutflow);
        request.setAttribute("totalOutflow", totalOutflow);
        request.setAttribute("incrementMonth", incrementMonth);

        request.getRequestDispatcher("details.jsp").forward(request, response);
    }

    private static double calculateProjectedAveragePf(LocalDate currentDate, LocalDate retirementDate, int incrementMonth, PayComponents pay) {
        List<Double> monthlyPf = new ArrayList<>();
        LocalDate datePointer = currentDate.withDayOfMonth(1);

        while (!datePointer.isAfter(retirementDate)) {
            double pf = applyPfHikeRules(pay, datePointer, incrementMonth);
            monthlyPf.add(pf);
            datePointer = datePointer.plusMonths(1);
        }

        
        int months = Math.min(60, monthlyPf.size());
        return months == 0 ? 0 :
                monthlyPf.subList(monthlyPf.size() - months, monthlyPf.size())
                         .stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static Map<String, Integer> getYearWisePfProjection(LocalDate currentDate, LocalDate retirementDate, int incrementMonth, PayComponents pay) {
        Map<String, Integer> projections = new LinkedHashMap<>();
        LocalDate datePointer = currentDate.withDayOfMonth(1);

        while (!datePointer.isAfter(retirementDate)) {
            double pf = applyPfHikeRules(pay, datePointer, incrementMonth);
            if (datePointer.getMonthValue() == 12) {
                projections.put(String.valueOf(datePointer.getYear()), (int) Math.round(pf));
                datePointer = datePointer.plusYears(1).withMonth(1);
            } else {
                datePointer = datePointer.plusMonths(1);
            }
        }
        
        System.out.println("Month: " + datePointer + ", PF Pay: " + pay.getPfPay());
        return projections;
    }

    private static Map<String, Double> calculateYearlyOutflow(Map<String, Integer> projections, LocalDate retirementDate) {
        Map<String, Double> outflows = new LinkedHashMap<>();
        double balance = 0;

        for (Map.Entry<String, Integer> entry : projections.entrySet()) {
            String year = entry.getKey();
            int pfValue = entry.getValue();

            int months = 12;
            if (Integer.parseInt(year) == retirementDate.getYear()) {
                months = retirementDate.getMonthValue();
            }

            double contribution = (months * (pfValue - 15000) * 0.0949) + (months * 15000 * 0.0833);
            balance += contribution;
            balance *= 1.0825;
            outflows.put(year, Math.round(balance * 100.0) / 100.0);
        }

        return outflows;
    }

    private static double applyPfHikeRules(PayComponents pay, LocalDate date, int incrementMonth) {
        int year = date.getYear(), month = date.getMonthValue();

        //Pay scale  2030
        if (year == 2030 && month == 1) {
            pay.basic *= 1.87;
            pay.da = pay.basic*0.02;
            System.out.println("Year-"+year+", Basic-"+pay.basic+", DA-"+pay.da);
        //Pay scale 2040    
        } else if (year == 2040 && month == 1) {
            pay.basic *= 1.4;
            pay.da = pay.basic*0.01;
            System.out.println("Year-"+year+", Basic-"+pay.basic+", DA-"+pay.da);
            
        } 
        
        else if (year < 2030) {
        	//Apply increment
        	if (month == incrementMonth) pay.basic *= 1.03;
        	
            //Apply DA increase
            if (month == 1) pay.da += pay.basic * 0.05;
            
            System.out.println("Year-"+year+", Basic-"+pay.basic+", DA-"+pay.da);
        } else if (year >= 2031 && year <= 2040) {
        	//Apply increment
        	if (month == incrementMonth) pay.basic *= 1.03;
        	//Apply DA increase
        	if (month == 1) pay.da += pay.basic * 0.02;
        	System.out.println("Year-"+year+", Basic-"+pay.basic+", DA-"+pay.da);
        } else if (year > 2040) {
        	//Apply increment
        	if (month == incrementMonth) pay.basic *= 1.03;
        	//Apply DA increase
        	if (month == 1) pay.da += pay.basic * 0.005;
        	System.out.println("Year-"+year+", Basic-"+pay.basic+", DA-"+pay.da);
        }

        return pay.getPfPay();
    }
}
