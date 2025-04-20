<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    LocalDate exitDate = (LocalDate) request.getAttribute("exitDate");
    String formattedExitDate = exitDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
%>
<!DOCTYPE html>
<html>
<head>
    <title>Pension Result</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
<div class="container">
    <h2>Pension Details</h2>

    <h3>Employee Info</h3>
    <p><strong>Employee ID:</strong> <%= request.getAttribute("empId") %></p>
    <p><strong>Employee Name:</strong> <%= request.getAttribute("empName") %></p>

  
    <p><strong>Pensionable Service Years:</strong> <%= request.getAttribute("serviceYears") %></p>
    <p><strong>Highest Salary Till 31/08/2014:</strong> ₹<%= request.getAttribute("highestSalaryTill2014") %></p>
    <p><strong>Average Salary entered by you for last 5 Years of your service till you turn 58 years:</strong> ₹<%= request.getAttribute("avgSalary5Yr") %></p>
    <p><strong>Exit Date:</strong> <%= formattedExitDate %></p>

    <h3>Calculated Pension</h3>
    <%
    double pensionAmount = (double) request.getAttribute("pension");
    long roundedPension = Math.round(pensionAmount);
%>
<p><strong>Estimated Pension:</strong> <span class="pension-amount">₹<%= roundedPension %></span></p>
	
	<p class="disclaimer">
    Disclaimer: This calculation is totally based on the last 5 year's average salary entered by user, therefore DPL does not take any responsibility of pension amount whatsoever.
</p>
    <a href="index.jsp">← Calculate Again</a>
</div>
</body>
</html>
