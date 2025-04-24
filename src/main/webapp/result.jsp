<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.time.temporal.ChronoUnit" %>
<%
    LocalDate exitDate = (LocalDate) request.getAttribute("exitDate");
    String formattedExitDate = exitDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    double serviceYears = (double) request.getAttribute("serviceYears");
    double highestSalaryTill2014 = (double) request.getAttribute("highestSalaryTill2014");
    double avgSalary5Yr = (double) request.getAttribute("avgSalary5Yr");
    double pensionAmount = (double) request.getAttribute("pension");
    long roundedPension = Math.round(pensionAmount);
    
    // Calculate breakdown values
    double serviceWithAdditionalYears = serviceYears + 2;
    long serviceDays = (long) (serviceWithAdditionalYears * 365);
    double firstPart = serviceDays * highestSalaryTill2014;
    
    LocalDate startDate = LocalDate.of(2014, 9, 1);
    String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    long daysAfter2014 = ChronoUnit.DAYS.between(startDate, exitDate);
    double secondPart = daysAfter2014 * avgSalary5Yr;
    
    double totalNumerator = firstPart + secondPart;
    double totalDenominator = 70 * 365;
%>
<!DOCTYPE html>
<html>
<head>
    <title>Pension Result</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .container {
            max-width: 900px;
            margin: 0 auto;
            padding: 20px;
            font-family: Arial, sans-serif;
            line-height: 1.6;
        }
        h2, h3 {
            color: #2a6496;
            margin-top: 20px;
        }
        .breakdown {
            margin: 20px 0;
            padding: 20px;
            background-color: #f5f5f5;
            border-radius: 5px;
            border: 1px solid #ddd;
        }
        .breakdown-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 15px;
        }
        .breakdown-table th, .breakdown-table td {
            padding: 10px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }
        .breakdown-table th {
            background-color: #e9e9e9;
            font-weight: bold;
        }
        .pension-amount {
            font-size: 1.8em;
            color: #2a6496;
            font-weight: bold;
            margin: 10px 0;
        }
        .formula {
            font-family: monospace;
            background-color: #f8f8f8;
            padding: 12px;
            border-radius: 5px;
            display: block;
            white-space: pre-wrap;
            word-wrap: break-word;
            margin: 10px 0;
            line-height: 1.6;
            border: 1px solid #e1e1e1;
            overflow-x: auto;
        }
        .formula-container {
            margin: 20px 0;
        }
        .formula-title {
            font-weight: bold;
            margin-bottom: 5px;
        }
        .disclaimer {
            margin-top: 30px;
            padding: 15px;
            background-color: #fff8e1;
            border-left: 4px solid #ffc107;
            font-size: 0.9em;
        }
        a {
            display: inline-block;
            margin-top: 20px;
            color: #2a6496;
            text-decoration: none;
            font-weight: bold;
        }
        a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
<div class="container">
    <h2>Pension Calculation Result</h2>

    <h3>Employee Information</h3>
    <p><strong>Employee ID:</strong> <%= request.getAttribute("empId") %></p>
    <p><strong>Employee Name:</strong> <%= request.getAttribute("empName") %></p>
    <p><strong>Pensionable Service Years (till 31-08-2014):</strong> <%= serviceYears %> years</p>
    <p><strong>Highest Salary Till 31-08-2014:</strong> ₹ <%= String.format("%,.2f", highestSalaryTill2014) %></p>
    <p><strong>Average Salary for Last 60 months:</strong> ₹ <%= String.format("%,.2f", avgSalary5Yr) %></p>
    <p><strong>Retirement Date:</strong> <%= formattedExitDate %></p>

    <div class="breakdown">
        <h3>Calculation Breakdown</h3>
        <div class="formula-container">
            <div class="formula-title">Pension Formula:</div>
            <div class="formula">
[(Service Days × Highest PF Pay Till August 2014) + 
(Days After August 2014 × Average monthly PF Pay of last 60 months till you turn 58 years )] 
/ (70 × 365)
            </div>
        </div>
        
        <table class="breakdown-table">
            <tr>
                <th>Component</th>
                <th>Calculation</th>
                <th>Value</th>
            </tr>
            <tr>
                <td>Service Years (with 2 additional years)</td>
                <td><%= serviceYears %> years + 2 years</td>
                <td><%= String.format("%.2f", serviceWithAdditionalYears) %> years</td>
            </tr>
            <tr>
                <td>Service Days</td>
                <td><%= String.format("%.2f", serviceWithAdditionalYears) %> years × 365 days</td>
                <td><%= serviceDays %> days</td>
            </tr>
            <tr>
                <td>First Part (Till 31-08-2014)</td>
                <td><%= serviceDays %> days × ₹<%= String.format("%,.2f", highestSalaryTill2014) %></td>
                <td>₹<%= String.format("%,.2f", firstPart) %></td>
            </tr>
            <tr>
                <td>Days After 01-09-2014</td>
                <td>From 01-09-2014 to <%= formattedExitDate %></td>
                <td><%= daysAfter2014 %> days</td>
            </tr>
            <tr>
                <td>Second Part (After 31-08-2014)</td>
                <td><%= daysAfter2014 %> days × ₹<%= String.format("%,.2f", avgSalary5Yr) %></td>
                <td>₹<%= String.format("%,.2f", secondPart) %></td>
            </tr>
            <tr>
                <td>Total Numerator</td>
                <td>First Part + Second Part</td>
                <td>₹<%= String.format("%,.2f", totalNumerator) %></td>
            </tr>
            <tr>
                <td>Total Denominator</td>
                <td>70 × 365</td>
                <td><%= totalDenominator %></td>
            </tr>
        </table>
    </div>

    <h3>Final Pension Calculation</h3>
    <div class="formula-container">
        <div class="formula">
₹ <%= String.format("%,.2f", totalNumerator) %> / <%= totalDenominator %> 
= ₹ <%= String.format("%,.2f", pensionAmount) %> per month
        </div>
    </div>
    <p><strong>Estimated Monthly Pension (rounded):</strong> <span class="pension-amount">₹ <%= String.format("%,d", roundedPension) %></span></p>
    
    <p class="disclaimer">
        <strong>Disclaimer:</strong> This calculation is based on the last 60 month's average salary entered by user. 
        The actual pension amount may vary based on official (EPFO) verification, calculations, NCP Days etc. 
        This calculator software does not take any responsibility for the accuracy of this pension amount.
    </p>
    
    <a href="index.jsp">← Calculate Again</a>
</div>
</body>
</html>