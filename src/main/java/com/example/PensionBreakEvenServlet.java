package com.example;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/PensionBreakEvenServlet")
public class PensionBreakEvenServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        double lumpSum = Double.parseDouble(request.getParameter("lumpSum"));
        double monthlyPension = Double.parseDouble(request.getParameter("monthlyPension"));
        double fdRate = Double.parseDouble(request.getParameter("fdRate"));
        double rdRate = Double.parseDouble(request.getParameter("rdRate"));

        ComparisonResult result = PensionCalculator.calculateAndCompare(
                lumpSum, monthlyPension, fdRate, rdRate);

        request.setAttribute("result", result);
        request.setAttribute("lumpSum", lumpSum);
        request.setAttribute("monthlyPension", monthlyPension);
        request.setAttribute("fdRate", fdRate);
        request.setAttribute("rdRate", rdRate);

        RequestDispatcher dispatcher = request.getRequestDispatcher("break-even-result.jsp");
        dispatcher.forward(request, response);
    }

}
