<%@ page import="java.util.*, com.example.*" %>
<html>
<head>
    <title>Pension Break-even Result</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        
        .header {
            background-color: #2c3e50;
            color: white;
            padding: 15px 20px;
            border-radius: 5px;
            margin-bottom: 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .home-link {
            color: white;
            text-decoration: none;
            font-weight: bold;
            padding: 8px 15px;
            background-color: #3498db;
            border-radius: 4px;
            transition: background-color 0.3s;
        }
        
        .home-link:hover {
            background-color: #2980b9;
        }
        
        .card {
            background-color: white;
            border-radius: 5px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            padding: 20px;
            margin-bottom: 20px;
        }
        
        h2, h3 {
            color: #2c3e50;
            margin-top: 0;
        }
        
        .input-summary {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 15px;
            margin-bottom: 25px;
        }
        
        .input-summary p {
            margin: 8px 0;
            padding: 10px;
            background-color: #f8f9fa;
            border-left: 4px solid #3498db;
        }
        
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        
        th {
            background-color: #2c3e50;
            color: white;
            padding: 12px;
            text-align: center;
        }
        
        td {
            padding: 10px;
            border: 1px solid #ddd;
            text-align: center;
        }
        
        tr:nth-child(even) {
            background-color: #f8f9fa;
        }
        
        tr:hover {
            background-color: #e9f7fe;
        }
        
        .highlight {
            font-weight: bold;
            color: #e74c3c;
        }
        
        .result-summary {
            margin-top: 30px;
        }
        
        .result-summary p {
            padding: 8px;
            margin: 5px 0;
            background-color: #e8f4f8;
            border-left: 4px solid #3498db;
        }
        
        .break-even {
            font-size: 1.2em;
            color: #27ae60;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <div class="header">
        <h2>Pension Break-even Analysis Report</h2>
        <a href="/pensiondpl/break-even.jsp" class="home-link">Home</a>
    </div>

    <div class="card">
        <h3>Input Parameters</h3>
        <div class="input-summary">
            <p><strong>Lump Sum Amount:</strong> Rs <%= String.format("%.2f", request.getAttribute("lumpSum")) %></p>
            <p><strong>Monthly Pension:</strong> Rs <%= String.format("%.2f", request.getAttribute("monthlyPension")) %></p>
            <p><strong>FD Interest Rate:</strong> <%= String.format("%.2f", request.getAttribute("fdRate")) %>%</p>
            <p><strong>RD Interest Rate:</strong> <%= String.format("%.2f", request.getAttribute("rdRate")) %>%</p>
        </div>
    </div>

    <div class="card">
        <h3>Year-by-Year Comparison</h3>
        <table>
            <thead>
                <tr>
                    <th>Year</th>
                    <th>Pension + RD (Rs)</th>
                    <th>FD Only (Rs)</th>
                    <th>Difference (Rs)</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
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
            </tbody>
        </table>
    </div>

    <div class="card result-summary">
        <h3>Final Results</h3>
        <%
            if (result != null) {
                int years = result.months / 12;
                int remMonths = result.months % 12;
        %>
            <p class="break-even">Break-even occurs after <%= years %> years and <%= remMonths %> months</p>
            <p><strong>Total Pension+RD value:</strong> Rs <%= String.format("%,.2f", result.rdValue) %></p>
            <p><strong>FD only value:</strong> Rs <%= String.format("%,.2f", result.fdValue) %></p>
            <p><strong>Difference at break-even:</strong> Rs <%= String.format("%,.2f", result.rdValue - result.fdValue) %></p>
        <%
            } else {
        %>
            <p class="highlight">The break-even point exceeds 100 years. Pension option may not be financially optimal.</p>
        <%
            }
        %>
    </div>
</body>
</html>