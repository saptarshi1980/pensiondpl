package com.example;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.servlet.*;
import javax.servlet.http.*;

public class CalculatePensionServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String empId = request.getParameter("empId");
        String empName = request.getParameter("empName");
        double serviceYears = Double.parseDouble(request.getParameter("serviceYears"));
        double highestSalaryTill2014 = Double.parseDouble(request.getParameter("highestSalaryTill2014"));
        String exitDateStr = request.getParameter("retirementMonthEnd");
        System.out.println("$$$ Exit date str:"+exitDateStr);
        double avgSalary5Yr = Double.parseDouble(request.getParameter("avgSalary"));

        // Parse exit date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate exitDate = LocalDate.parse(exitDateStr, formatter);

        // Calculate pension
        PensionService pensionService = new PensionService();
        double pension = pensionService.calculatePension(serviceYears, highestSalaryTill2014, exitDate, avgSalary5Yr);

        // Set attributes for JSP
        request.setAttribute("empId", empId);
        request.setAttribute("serviceYears", serviceYears);
        request.setAttribute("highestSalaryTill2014", highestSalaryTill2014);
        request.setAttribute("exitDate", exitDate);
        request.setAttribute("avgSalary5Yr", avgSalary5Yr);
        request.setAttribute("pension", pension);
        request.setAttribute("empName", empName);
        

        // Forward to result page
        RequestDispatcher dispatcher = request.getRequestDispatcher("result.jsp");
        dispatcher.forward(request, response);
    }
}
