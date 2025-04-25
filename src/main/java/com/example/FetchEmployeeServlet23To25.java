package com.example;

import com.example.PFMonthlyCalculation;
import com.example.PFCalculator.PaySlip;
import com.example.util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@WebServlet("/FetchEmployeeServlet23To251")
public class FetchEmployeeServlet23To25 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String empId = req.getParameter("empId");
        double openingBalance = Double.parseDouble(req.getParameter("demand_amt"));
 
        try (Connection con = DBUtil.getConnection()) {
        	
        	
            List<PaySlip> paySlips = new PFCalculator().fetchPaySlips(con, empId);
            Map<String, PFMonthlyCalculation> result = new PFCalculator().calculatePFProjection(paySlips, openingBalance);
            String empName = paySlips.isEmpty() ? "Unknown" : paySlips.get(0).getEmpName();
            
            req.setAttribute("pfResults", result);
            req.setAttribute("empName", empName);
            req.getRequestDispatcher("pfResults.jsp").forward(req, resp);
        } catch (SQLException | NumberFormatException e) {
            req.setAttribute("error", "Something went wrong: " + e.getMessage());
            req.getRequestDispatcher("error.jsp").forward(req, resp);
        }
    }
}
