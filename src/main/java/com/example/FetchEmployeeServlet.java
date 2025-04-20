	package com.example;
	
	import com.example.util.DBUtil;
	
	import java.io.IOException;
	import java.sql.*;
	import java.time.LocalDate;
	import java.time.format.DateTimeFormatter;
	import java.util.*;
	
	import javax.servlet.*;
	import javax.servlet.http.*;
	
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
	
	        try (Connection con = DBUtil.getConnection()) {
	
	            // Query 1: Get base PF and retirement details for Aug 2014
	            String query =
	                "SELECT " +
	                "    e.emp_name, " +
	                "    TO_CHAR(e.birth_date, 'dd-mm-yyyy') AS dob, " +
	                "    TO_CHAR(LAST_DAY(ADD_MONTHS(e.birth_date, 58 * 12)), 'dd-mm-yyyy') AS retirement_month_end, " +
	                "    p.pf_pay, " +
	                "    p.join_date, " +
	                "    FLOOR(MONTHS_BETWEEN(TO_DATE('31-08-2014', 'dd-mm-yyyy'), TO_DATE(p.join_date, 'dd-mm-yyyy')) / 12 + 0.5) AS service_years " +
	                "FROM app_vw_emp e " +
	                "JOIN vw_dcpy_dpl_payslip p ON e.emp_id = p.ngs " +
	                "WHERE p.ngs = ? " +
	                "AND p.sal_month = 8 " +
	                "AND p.sal_year = 2014";
	
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
	                    }
	                }
	            }
	
	            // Query 2: Get latest PF pay for given employee
	            String latestPfQuery =
	                "SELECT pf_pay " +
	                "FROM vw_dcpy_dpl_payslip " +
	                "WHERE ngs = ? " +
	                "AND (sal_year, sal_month) = ( " +
	                "    SELECT sal_year, sal_month " +
	                "    FROM ( " +
	                "        SELECT sal_year, sal_month " +
	                "        FROM vw_dcpy_dpl_payslip " +
	                "        WHERE ngs = ? " +
	                "        ORDER BY sal_year DESC, sal_month DESC " +
	                "    ) " +
	                "    WHERE ROWNUM = 1 " +
	                ")";
	
	            try (PreparedStatement ps = con.prepareStatement(latestPfQuery)) {
	                ps.setString(1, empId);
	                ps.setString(2, empId);
	                try (ResultSet rs = ps.executeQuery()) {
	                    if (rs.next()) {
	                        latestPfPay = rs.getDouble("pf_pay");
	                    }
	                }
	            }
	
	            // --- Calculate projected average PF and get yearly projections ---
	            if (retirementMonthEnd != null && latestPfPay > 0) {
	                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	                LocalDate retirementDate = LocalDate.parse(retirementMonthEnd, formatter);
	                LocalDate currentDate = LocalDate.now();
	                projectedAvgPf = calculateProjectedAveragePf(latestPfPay, currentDate, retirementDate);
	                yearlyProjections = getYearWisePfProjection(latestPfPay, currentDate, retirementDate);
	            }
	
	        } catch (Exception e) {
	            e.printStackTrace();
	            throw new ServletException("Database error occurred", e);
	        }
	
	        // Set attributes for JSP
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
	
	        request.getRequestDispatcher("details.jsp").forward(request, response);
	    }
	
	    public static double calculateProjectedAveragePf(double currentPf, LocalDate currentDate, LocalDate retirementDate) {
	        List<Double> yearlyPfPays = new ArrayList<>();
	        double pf = currentPf;

	        LocalDate yearPointer = LocalDate.of(2025, 1, 1);  // Start from 2025
	        LocalDate jan2029 = LocalDate.of(2029, 1, 1);

	        while (yearPointer.isBefore(jan2029) && yearPointer.isBefore(retirementDate)) {
	            if (yearPointer.getYear() == 2025) {
	                yearlyPfPays.add(pf);  // No hike in 2025
	            } else {
	                pf *= 1.08;  // 8% hike from 2026 to 2028
	                yearlyPfPays.add(pf);
	            }
	            yearPointer = yearPointer.plusYears(1);
	        }

	        if (!yearPointer.isAfter(jan2029) && yearPointer.isBefore(retirementDate)) {
	            pf *= 1.5;  // 50% hike in 2029
	            yearlyPfPays.add(pf);
	            yearPointer = yearPointer.plusYears(1);
	        }

	        while (yearPointer.isBefore(retirementDate)) {
	            pf *= 1.04;  // 4% hike from 2030 onwards
	            yearlyPfPays.add(pf);
	            yearPointer = yearPointer.plusYears(1);
	        }

	        int size = yearlyPfPays.size();
	        if (size < 5) {
	            throw new IllegalStateException("Less than 5 years of PF data projected.");
	        }

	        List<Double> lastFiveYears = yearlyPfPays.subList(size - 5, size);
	        return lastFiveYears.stream().mapToDouble(Double::doubleValue).average().orElse(0);
	    }

	    public static Map<String, Integer> getYearWisePfProjection(double currentPf, LocalDate currentDate, LocalDate retirementDate) {
	        Map<String, Integer> yearWiseProjection = new LinkedHashMap<>();
	        double pf = currentPf;

	        LocalDate yearPointer = LocalDate.of(2025, 1, 1);  // Start from 2025

	        while (yearPointer.getYear() <= retirementDate.getYear()) {
	            int year = yearPointer.getYear();

	            if (year == 2025) {
	                // No hike in 2025
	                yearWiseProjection.put(String.valueOf(year), (int) Math.round(pf));
	            } else if (year >= 2026 && year <= 2029) {
	                // 8% hike from 2026 to 2029
	                pf *= 1.08;
	                yearWiseProjection.put(String.valueOf(year), (int) Math.round(pf));
	            } else if (year == 2030) {
	                // 1.5x hike in 2030
	                pf *= 1.5;
	                yearWiseProjection.put(String.valueOf(year), (int) Math.round(pf));
	            } else {
	                // 4% hike from 2031 onward
	                pf *= 1.04;
	                yearWiseProjection.put(String.valueOf(year), (int) Math.round(pf));
	            }

	            yearPointer = yearPointer.plusYears(1);
	        }

	        return yearWiseProjection;
	    }


	}
