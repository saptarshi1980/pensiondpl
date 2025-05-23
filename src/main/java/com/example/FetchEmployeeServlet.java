package com.example;

import com.example.util.DBUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;

public class FetchEmployeeServlet extends HttpServlet {

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

	// Add this static field to store the projections during calculateYearlyOutflow
	// execution
	private static Map<String, PayComponents> lastCalculatedProjections = new LinkedHashMap<>();

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// Create a StringWriter to capture the console output
		StringWriter stringWriter = new StringWriter();
		PrintWriter logWriter = new PrintWriter(stringWriter);

		String empId = request.getParameter("empId");
		System.out.println("**"+request.getParameter("demand_amt"));
		double demandAmt = Double.parseDouble(request.getParameter("demand_amt"));
		boolean downloadReport = "true".equals(request.getParameter("downloadReport"));

		String empName = null, dob = null, retirementMonthEnd = null, joinDate = null;
		double pfPay = 0, projectedAvgPf = 0, totalOutflow = 0;
		int serviceDays = 0, incrementMonth = 1,total_service_days=0;
		PayComponents payComponents = new PayComponents(0, 0);
		Map<String, PayComponents> yearlyProjections = new LinkedHashMap<>();
		Map<String, Double> yearlyOutflow = new LinkedHashMap<>();

		try (Connection con = DBUtil.getConnection()) {

			// Fetch employee & salary info
			/*
			 * String query =
			 * "SELECT e.emp_name, TO_CHAR(e.birth_date, 'dd-mm-yyyy') AS dob, " +
			 * "TO_CHAR(LAST_DAY(ADD_MONTHS(e.birth_date, 58 * 12)), 'dd-mm-yyyy') AS retirement_month_end, "
			 * + "p.pf_pay, p.join_date, " +
			 * "TO_DATE('31-08-2014', 'dd-mm-yyyy') - TO_DATE(p.join_date, 'dd-mm-yyyy') AS service_days "
			 * + "FROM app_vw_emp e JOIN vw_dcpy_dpl_payslip p ON e.emp_id = p.ngs " +
			 * "WHERE p.ngs = ? AND p.sal_month = 8 AND p.sal_year = 2014";
			 */
			
			//String query ="SELECT e.emp_name, TO_CHAR(e.birth_date, 'dd-mm-yyyy') AS dob, TO_CHAR(LAST_DAY(ADD_MONTHS(e.birth_date, 58 * 12)), 'dd-mm-yyyy') AS retirement_month_end, p.pf_pay, p.join_date, TO_DATE('31-08-2014', 'dd-mm-yyyy') - GREATEST(TO_DATE(p.join_date, 'dd-mm-yyyy'), TO_DATE('01-09-1995', 'dd-mm-yyyy')) AS service_days FROM app_vw_emp e JOIN vw_dcpy_dpl_payslip p ON e.emp_id = p.ngs WHERE p.ngs = ? AND p.sal_month = 8 AND p.sal_year = 2014";
			
			String query = "SELECT e.emp_name, TO_CHAR(e.birth_date, 'dd-mm-yyyy') AS dob, TO_CHAR(LAST_DAY(ADD_MONTHS(e.birth_date, 58*12)), 'dd-mm-yyyy') AS retirement_date, p.pf_pay, p.join_date, (TO_DATE('31-08-2014', 'dd-mm-yyyy') - GREATEST(TO_DATE(p.join_date, 'dd-mm-yyyy'), TO_DATE('01-09-1995', 'dd-mm-yyyy'))) AS days_until_2014, (LAST_DAY(ADD_MONTHS(e.birth_date, 58*12)) - GREATEST(TO_DATE(p.join_date, 'dd-mm-yyyy'), TO_DATE('01-09-1995', 'dd-mm-yyyy'))) AS total_days_until_retirement, CASE WHEN (LAST_DAY(ADD_MONTHS(e.birth_date, 58*12)) - GREATEST(TO_DATE(p.join_date, 'dd-mm-yyyy'), TO_DATE('01-09-1995', 'dd-mm-yyyy')))/365 > 20 THEN (LAST_DAY(ADD_MONTHS(e.birth_date, 58*12)) - GREATEST(TO_DATE(p.join_date, 'dd-mm-yyyy'), TO_DATE('01-09-1995', 'dd-mm-yyyy')))/365 + 2 ELSE (LAST_DAY(ADD_MONTHS(e.birth_date, 58*12)) - GREATEST(TO_DATE(p.join_date, 'dd-mm-yyyy'), TO_DATE('01-09-1995', 'dd-mm-yyyy')))/365 END AS epfo_pensionable_years, SUBSTR(c.nextincr,1,2) AS incr_month FROM (SELECT DISTINCT emp_id, emp_name, birth_date FROM app_vw_emp WHERE emp_id = ?) e JOIN (SELECT DISTINCT ngs, pf_pay, join_date FROM vw_dcpy_dpl_payslip WHERE ngs = ? AND sal_month = 8 AND sal_year = 2014) p ON e.emp_id = p.ngs JOIN (SELECT DISTINCT ngs, nextincr FROM vw_dcpyint_paybill WHERE ngs = ? AND sal_month = 8 AND sal_year = 2014) c ON p.ngs = c.ngs";
			//System.out.println("Query-"+query);
			
			try (PreparedStatement ps = con.prepareStatement(query)) {
				ps.setString(1, empId);
				ps.setString(2, empId);
			    ps.setString(3, empId);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						empName = rs.getString("emp_name");
						dob = rs.getString("dob");
						retirementMonthEnd = rs.getString("retirement_date");
						pfPay = rs.getDouble("pf_pay");
						joinDate = rs.getString("join_date");
						serviceDays = rs.getInt("days_until_2014");
						incrementMonth= rs.getInt("incr_month");
						total_service_days = rs.getInt("total_days_until_retirement");
						//System.out.println("Increment moth-"+incrementMonth);

						/*
						 * if (joinDate != null && !joinDate.isEmpty()) { LocalDate joinLocalDate =
						 * LocalDate.parse(joinDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
						 * incrementMonth = joinLocalDate.getMonthValue();
						 * System.out.println("Increment moth-"+incrementMonth); //
						 * logWriter.println("Increment month fetched from data: " + incrementMonth); }
						 */
					}
					else {
						request.getRequestDispatcher("index.jsp").forward(request, response);
					}
				}
			}

			// Fetch latest PF details
			String latestPfQuery = "SELECT pf_pay, basic_salary, dearness_allowance FROM vw_dcpy_dpl_payslip WHERE ngs = ? "
					+ "AND (sal_year, sal_month) = (SELECT sal_year, sal_month FROM ( "
					+ "SELECT sal_year, sal_month FROM vw_dcpy_dpl_payslip "
					+ "WHERE ngs = ? ORDER BY sal_year DESC, sal_month DESC) WHERE ROWNUM = 1)";

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
			logWriter.println(
					"==============================================================================================================");
			logWriter.println("                                       PF PROJECTION REPORT");
			logWriter.println(
					"==============================================================================================================");
			logWriter.println("Employee ID: " + empId);
			logWriter.println("Employee Name: " + empName);
			logWriter.println("Date of Birth: " + dob);
			logWriter.println("Retirement Date: " + retirementMonthEnd);
			logWriter.println("Service Days before Sept 2014: " + serviceDays);
			logWriter.println("EPFO Demand amount / Opening Balance of PF as on April 2025: " + demandAmt);
			logWriter.println(
					"==============================================================================================================");

			if (retirementMonthEnd != null && payComponents.getPfPay() > 0) {
				LocalDate retirementDate = LocalDate.parse(retirementMonthEnd,
						DateTimeFormatter.ofPattern("dd-MM-yyyy"));
				LocalDate currentDate = LocalDate.now();

				// First, calculate yearly outflow - this will also populate
				// lastCalculatedProjections
				yearlyOutflow = calculateYearlyOutflow(payComponents, retirementDate, logWriter, empId, empName,
						incrementMonth, demandAmt);

				// Then extract projections from the calculateYearlyOutflow method's internal
				// calculations
				yearlyProjections = extractYearlyProjections();

				// Calculate projected average PF
				
				String cutoffDateStr = "30-04-2025";
				
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		        
		        try {
		            Date retirementDatedt = sdf.parse(retirementMonthEnd);
		            Date cutoffDate = sdf.parse(cutoffDateStr);
		            
		            
		            if (retirementDatedt.before(cutoffDate)) {
		            	
		            	projectedAvgPf = calculateAveragePfPay(empId);
		            	
		                
		            } else if (retirementDatedt.after(cutoffDate)) {
		            	
		            	
		            	projectedAvgPf = calculateProjectedAveragePf(currentDate, retirementDate, incrementMonth,
								new PayComponents(payComponents.basic, payComponents.da), logWriter);
		            } else {
		                projectedAvgPf = calculateAveragePfPay(empId);
		            }
		        } catch (ParseException e) {
		            System.err.println("Invalid date format: " + e.getMessage());
		        }
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				

				totalOutflow = yearlyOutflow.values().stream().mapToDouble(Double::doubleValue).sum();
				System.out.println("Total outlfow:"+totalOutflow);
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
			request.setAttribute("demand_amt", demandAmt);
			request.setAttribute("empName", empName);
			request.setAttribute("dob", dob);
			request.setAttribute("retirementMonthEnd", retirementMonthEnd);
			request.setAttribute("pfPay", pfPay);
			request.setAttribute("joinDate", joinDate);
			request.setAttribute("serviceDays", serviceDays);
			request.setAttribute("totalServiceDays", total_service_days);
			request.setAttribute("latestPfPay", payComponents.getPfPay());
			request.setAttribute("projectedAvgPf", projectedAvgPf);
			request.setAttribute("yearlyProjections", yearlyProjections);
			request.setAttribute("yearlyOutflow", yearlyOutflow);
	        NumberFormat nf = NumberFormat.getInstance();
	        nf.setMaximumFractionDigits(0);
	        double netOutflow = 0;
	        if (yearlyOutflow != null && !yearlyOutflow.isEmpty()) {
	            netOutflow = yearlyOutflow.get(yearlyOutflow.keySet().toArray()[yearlyOutflow.size() - 1]);
	        }
			request.setAttribute("totalOutflow", totalOutflow);
			String netOutflowWords = new NumberToWordConverter().convert((long)netOutflow);
	        request.setAttribute("netOutflowWords", netOutflowWords);
			request.setAttribute("incrementMonth", incrementMonth);
			request.setAttribute("consoleOutput", stringWriter.toString());

			request.getRequestDispatcher("details.jsp").forward(request, response);
		}
	}

	private static double calculateProjectedAveragePf(LocalDate currentDate, LocalDate retirementDate,
			int incrementMonth, PayComponents initialPay, PrintWriter logWriter) {
		List<Double> monthlyPf = new ArrayList<>();
		LocalDate datePointer = currentDate.withDayOfMonth(1);

		// Create a new copy to avoid modifying the original
		PayComponents currentPay = new PayComponents(initialPay);
		System.out.println("curr pay basic from projection"+currentPay.basic);
		System.out.println("curr pay basic from projection"+currentPay.da);

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
		if (months == 0)
			return 0;

		double sum = 0;
		for (int i = monthlyPf.size() - months; i < monthlyPf.size(); i++) {
			sum += monthlyPf.get(i);
		}

		return sum / months;
	}

	// Method to access the projections extracted during the last
	// calculateYearlyOutflow run
	private static Map<String, PayComponents> extractYearlyProjections() {
		// Return a copy of the stored projections to prevent modification
		Map<String, PayComponents> result = new LinkedHashMap<>();
		for (Map.Entry<String, PayComponents> entry : lastCalculatedProjections.entrySet()) {
			result.put(entry.getKey(), new PayComponents(entry.getValue()));
		}
		return result;
	}

	private static Map<String, Double> calculateYearlyOutflow(PayComponents initialPay, LocalDate retirementDate, 
	        PrintWriter logWriter, String empId, String empName, int incrementMonth, double openingPfBalance) {
	    
	    // Clear previous projections
	    System.out.println("Demand from calculate-"+openingPfBalance);
		lastCalculatedProjections.clear();
	    
	    Map<String, Double> yearlyOutflows = new LinkedHashMap<>();
	    double balance = openingPfBalance;  // Initialize with provided opening balance
	    double monthlyInterestRate = 0.0825 / 12;
	    double finalRetirementBalance = 0;
	    double accruedInterest = 0.0;  // Track interest that accrues but isn't credited yet

		logWriter.println("Formula used as follows");
	    logWriter.println(
	            "========================================================================================================================================================================================================================");
	    logWriter.println(
	            "If PF Pay exceeds Rs 15000/-, Contribution Outflow = (8.33% of PF Pay plus 1.16% of (PF Pay minus Rs 15,000) minus Rs 1,250). If PF Pay is Rs 15,000 or less, the contribution is zero.. Contribution accumulated year on year");
	    logWriter.println(
	            "========================================================================================================================================================================================================================");
	    logWriter.println(
	            "---------------------------------------------------------------------------------------------------------------------------------------------");
	    logWriter.printf("%-14s %14s %15s %15s %18s %18s %15s %15s %18s%n", "Financial Year", "Month", "Basic", "DA",
	            "Opening Bal", "Contribution", "Month Interest", "Accrued Int", "Closing Bal");

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
	        
	        // Calculate interest accrued this month (but not added to balance yet)
	        double monthlyInterest = round(openingBalance * monthlyInterestRate);
	        accruedInterest += monthlyInterest;
	        
	        // Add contribution to the balance
	        balance += netMonthlyContribution;
	        
	        // For March (end of financial year), add accrued interest to the balance
	        if (isLastMonth) {
	            balance += accruedInterest;
	            double closingBalanceAfterInterest = round(balance);
	            
	            // Format the financial year correctly for calculation
	            String financialYear = String.valueOf(year - 1);
	            yearlyOutflows.put(financialYear, Math.round(closingBalanceAfterInterest * 100.0) / 100.0);
	            
	            // Display the March entry with added interest
	            logWriter.printf("%-14s %14s %,15.2f %,15.2f %,18.2f %,18.2f %,15.2f %,15.2f %,18.2f%n", 
	                    financialYear+"-"+(Integer.parseInt(financialYear)+1),
	                    monthName,
	                    monthlyPay.getBasic(), 
	                    monthlyPay.getDa(), 
	                    openingBalance,
	                    netMonthlyContribution, 
	                    monthlyInterest, 
	                    accruedInterest, 
	                    openingBalance + netMonthlyContribution);
	                    
	            // Add a second row showing interest credit
	            logWriter.printf("%-14s %14s %,15.2f %,15.2f %,18.2f %,18.2f %,15.2f %,15.2f %,18.2f%n", 
	                    financialYear+"-"+(Integer.parseInt(financialYear)+1),
	                    monthName + "*", 
	                    monthlyPay.getBasic(), 
	                    monthlyPay.getDa(), 
	                    openingBalance + netMonthlyContribution,
	                    0.0, 
	                    0.0, 
	                    accruedInterest, 
	                    closingBalanceAfterInterest);
	            
	            logWriter.println(
	                    "--------------------------------------------------------------------------------------------------------------------------------------------------------");
	            logWriter.printf("%-14s %14s %15s %15s %18s %18s %15s %15s %,18.2f%n", 
	                    "Financial Year", 
	                    financialYear+"-"+(Integer.parseInt(financialYear)+1), 
	                    "", "", "", "Total:", "", "",
	                    yearlyOutflows.get(financialYear));
	            logWriter.println(
	                    "--------------------------------------------------------------------------------------------------------------------------------------------------------");
	                    
	            // Reset accrued interest after adding to balance
	            accruedInterest = 0.0;
	        } else {
	            // Display normal month entries
	            logWriter.printf("%-14s %14s %,15.2f %,15.2f %,18.2f %,18.2f %,15.2f %,15.2f %,18.2f%n", 
	                    (month >= 4) ? year+"-"+(year+1) : (year-1)+"-"+year,
	                    monthName,
	                    monthlyPay.getBasic(), 
	                    monthlyPay.getDa(), 
	                    openingBalance,
	                    netMonthlyContribution, 
	                    monthlyInterest, 
	                    accruedInterest, 
	                    balance);
	        }
	        
	        // Store December values for each year for projections
	        if (month == 12) {
	            lastCalculatedProjections.put(String.valueOf(year), new PayComponents(monthlyPay));
	        }

	        // Check if this is the last month before or equal to retirement
	        if (currentDate.getYear() == retirementDate.getYear() && currentDate.getMonthValue() == retirementDate.getMonthValue()) {
	            finalRetirementBalance = balance;
	            yearlyOutflows.put("retirement", Math.round(balance * 100.0) / 100.0);
	            
	            if (month != 12) {
	                lastCalculatedProjections.put(String.valueOf(year), new PayComponents(monthlyPay));
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
	    logWriter.printf("Final PF Balance at Retirement: %,15.2f%n", finalRetirementBalance);
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
	
	private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
	
	private static double applyPfHikeRules(PayComponents pay, LocalDate date, int incrementMonth,
			PrintWriter logWriter) {
		int year = date.getYear(), month = date.getMonthValue();

		// Apply regular yearly increment based on increment month for all years
		if (month == incrementMonth) {
			pay.basic *= 1.03;
			pay.basic = roundUpToNearest100(pay.basic);
			logWriter.println("Applying regular 3% increment in " + date.getMonth() + " " + year + " (Basic after: "
					+ pay.basic + ")");
		}

		// DA adjustments in January for different year ranges
		if (month == 1) {
			// Special case handling for 2030
			if (year == 2030) {
				logWriter.println("Applying special 2030 multiplier (1.87) and three 3% hikes in January " + year
						+ " (Basic before: " + pay.basic + ")");
				// Apply the special 1.87 multiplier
				pay.basic *= 1.87;
				// Apply three 3% hikes
				pay.basic = pay.basic * 1.03 * 1.03 * 1.03;
				pay.da = pay.basic * 0.02;
				logWriter.println("Basic after special 2030 adjustment: " + pay.basic);
			}
			// Special case handling for 2040
			else if (year == 2040) {
				logWriter.println("Applying special 2040 multiplier (1.4) and three 3% hikes in January " + year
						+ " (Basic before: " + pay.basic + ")");
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
					logWriter.println("Applying DA adjustment in January " + year + " (DA before: " + pay.da
							+ ", Increase: " + daIncrease + ")");
					pay.da += daIncrease;
					logWriter.println("DA after adjustment: " + pay.da);
				}
			}
		}

		return pay.getPfPay();
	}
	
	
	public static double roundUpToNearest100(double amount) {
	    return (Math.ceil(amount / 100) * 100);
	}
	
	public double calculateAveragePfPay(String employeeId) {
        String sql = "SELECT AVG(pf_pay) AS avg_pf_pay_last_60_months " +
                     "FROM vw_dcpy_dpl_payslip " +
                     "WHERE ngs = ? " +
                     "AND TO_DATE('01-'||sal_month||'-'||sal_year, 'dd-mm-yyyy') BETWEEN " +
                     "    ADD_MONTHS( " +
                     "        LEAST( " +
                     "            LAST_DAY(ADD_MONTHS( " +
                     "                (SELECT birth_date FROM app_vw_emp WHERE emp_id = ?), " +
                     "                58*12 " +
                     "            )), " +
                     "            TO_DATE('30-04-2025', 'dd-mm-yyyy') " +
                     "        ), " +
                     "        -60 " +
                     "    ) " +
                     "    AND " +
                     "    LEAST( " +
                     "        LAST_DAY(ADD_MONTHS( " +
                     "            (SELECT birth_date FROM app_vw_emp WHERE emp_id = ?), " +
                     "            58*12 " +
                     "        )), " +
                     "        TO_DATE('30-04-2025', 'dd-mm-yyyy') " +
                     "    )";

        double averagePfPay = 0.0;
        
        try (Connection conn =  DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set the employee ID parameter (used 3 times in the query)
            stmt.setString(1, employeeId);
            stmt.setString(2, employeeId);
            stmt.setString(3, employeeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    averagePfPay = rs.getDouble("avg_pf_pay_last_60_months");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return averagePfPay;
    }


}