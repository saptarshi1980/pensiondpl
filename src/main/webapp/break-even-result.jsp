<%@ page import="java.util.*, com.example.*" %>
<html>
<head>
    <title>Pension Break-even Result</title>
    <style>
        table { border-collapse: collapse; width: 100%; margin-top: 20px; }
        th, td { border: 1px solid #ccc; padding: 8px; text-align: center; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
<div style="padding: 10px; background-color: #e9ecef; text-align: center;">
  <a href="/pensiondpl/break-even.jsp" font-size: 18px; color: #333; font-weight: bold;">
    Home
  </a>
</div>
    <h2>Year-by-Year Comparison Report</h2>
    <table>
        <tr>
            <th>Year</th>
            <th>Pension + RD (Rs)</th>
            <th>FD Only (Rs)</th>
            <th>Difference (Rs)</th>
            <th>Status</th>
        </tr>
        <%
            ComparisonResult result = (ComparisonResult) request.getAttribute("result");
            if (result != null && result.getYearlyData() != null) {
                for (YearlyComparison row : result.getYearlyData()) {
        %>
        <tr>
            <td><%= row.getYearLabel() %></td>
            <td><%= String.format("%,.2f", row.getRdValue()) %></td>
            <td><%= String.format("%,.2f", row.getFdValue()) %></td>
            <td><%= String.format("%,.2f", row.getDifference()) %></td>
            <td><%= row.getStatus() %></td>
        </tr>
        <%
                }
            }
        %>
    </table>

    <h3>Final Results:</h3>
    <%
        if (result != null) {
            int years = result.months / 12;
            int remMonths = result.months % 12;
    %>
        <p>Break-even occurs after <%= years %> years and <%= remMonths %> months</p>
        <p>Total Pension+RD value: Rs <%= String.format("%,.2f", result.rdValue) %></p>
        <p>FD only value: Rs <%= String.format("%,.2f", result.fdValue) %></p>
        <p>Difference at break-even: Rs <%= String.format("%,.2f", result.rdValue - result.fdValue) %></p>
    <%
        } else {
    %>
        <p>The break-even point exceeds 100 years. Pension option may not be financially optimal.</p>
    <%
        }
    %>
</body>
</html>
