<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="com.example.FetchEmployeeServlet.PayComponents" %>
<!DOCTYPE html>
<html>
<head>
    <title>Employee Details</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            background-color: #f8f9fa;
            padding: 20px;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        .employee-container {
            max-width: 900px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.1);
        }
        .employee-header {
            color: #2c3e50;
            border-bottom: 2px solid #3498db;
            padding-bottom: 10px;
            margin-bottom: 25px;
        }
        .detail-row {
            display: flex;
            margin-bottom: 12px;
            padding: 10px;
            border-bottom: 1px solid #eee;
        }
        .detail-label {
            font-weight: 600;
            color: #3498db;
            width: 40%;
        }
        .detail-value {
            width: 60%;
        }
        .salary-input {
            background-color: #f8f9fa;
            padding: 25px;
            border-radius: 8px;
            margin: 25px 0;
        }
        .input-title {
            color: #6a1b9a;
            font-weight: 600;
            margin-bottom: 15px;
            font-size: 1.1rem;
        }
        .btn-calculate {
            background-color: #3498db;
            color: white;
            padding: 10px 25px;
            border: none;
            border-radius: 5px;
            font-weight: 500;
            transition: all 0.3s;
        }
        .btn-calculate:hover {
            background-color: #2980b9;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.1);
        }
        
        .center-page {
            display: flex;
            justify-content: center;
            align-items: center;
            
        }
        
        .btn-report {
            background-color: #C70039 ;
            color: white;
            padding: 10px 25px;
            border: none;
            border-radius: 5px;
            font-weight: 500;
            transition: all 0.3s;
        }
        
        .btn-report:hover {
            background-color: #581845 ;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.1);
        }
    </style>
</head>
<body>
<div class="employee-container">
    <h2 class="employee-header">Employee Details</h2>

    <div class="employee-details">
        <div class="detail-row">
            <div class="detail-label">Employee ID:</div>
            <div class="detail-value"><%= request.getAttribute("empId") %></div>
        </div>
        <div class="detail-row">
            <div class="detail-label">Name:</div>
            <div class="detail-value"><%= request.getAttribute("empName") %></div>
        </div>
        <div class="detail-row">
            <div class="detail-label">Date of Birth:</div>
            <div class="detail-value"><%= request.getAttribute("dob") %></div>
        </div>
        <div class="detail-row">
            <div class="detail-label">58 years Month-End:</div>
            <div class="detail-value"><%= request.getAttribute("retirementMonthEnd") %></div>
        </div>
        <div class="detail-row">
            <div class="detail-label">PF Pay (31/08/2014):</div>
            <div class="detail-value">₹ <%= request.getAttribute("pfPay") %></div>
        </div>
        <div class="detail-row">
            <div class="detail-label">Joining Date:</div>
            <div class="detail-value"><%= request.getAttribute("joinDate") %></div>
        </div>
        <div class="detail-row">
            <div class="detail-label">Service Years (till 31/08/2014):</div>
            <div class="detail-value"><%= request.getAttribute("serviceYears") %> years</div>
        </div>
    </div>

    <div class="detail-row">
        <div class="detail-label">Projected Average PF Pay (Last 5 years till the age of 58 years):</div>
        <div class="detail-value text-success fw-bold">
            ₹ <%= Math.round(Double.parseDouble(request.getAttribute("projectedAvgPf").toString())) %>
        </div>
    </div>

    <form action="CalculatePensionServlet" method="post" class="salary-input">
        <input type="hidden" name="empId" value="<%= request.getAttribute("empId") %>">
        <input type="hidden" name="empName" value="<%= request.getAttribute("empName") %>">
        <input type="hidden" name="serviceYears" value="<%= request.getAttribute("serviceYears") %>">
        <input type="hidden" name="highestSalaryTill2014" value="<%= request.getAttribute("pfPay") %>">
        <input type="hidden" name="retirementMonthEnd" value="<%= request.getAttribute("retirementMonthEnd") %>">

        <div class="input-title">
            If you think the above projection is incorrect, you can enter your own estimate of the average PF Pay for the last 5 years (up to age 58):
        </div>

        <div class="input-group mb-3">
            <span class="input-group-text">₹</span>
            <input type="number" class="form-control" name="avgSalary" id="avgSalary" 
                   value="<%= Math.round(Double.parseDouble(request.getAttribute("projectedAvgPf").toString())) %>"
                   required step="1" min="0" autofocus
                   placeholder="Enter average amount">
        </div>

        <div class="text-center">
            <button type="submit" class="btn-calculate">Calculate Pension</button>
        </div>
    </form>
    
    <div class="center-page">
        <form action="FetchEmployeeServlet" method="post">
            <input type="hidden" name="empId" value="${empId}">
            <input type="hidden" name="downloadReport" value="true">
            <button type="submit" class="btn-report">Download Month-wise PF Contribution Projection Report</button>
        </form>
    </div>

    <!-- Year-wise Projection Section -->
    <!-- Year-wise Projection Section -->
<div class="mt-5">
    <h4 class="text-primary mb-3">📅 Year-wise Projected PF Pay - Entirely Hypothetical</h4>
    <%
        Map<String, PayComponents> projections = (Map<String, PayComponents>) request.getAttribute("yearlyProjections");
        if (projections != null && !projections.isEmpty()) {
    %>
    <div class="table-responsive">
        <table class="table table-striped table-bordered table-hover">
            <thead class="table-light">
                <tr>
                    <th>End of Year</th>
                    <th>Basic Salary (₹)</th>
                    <th>DA (₹)</th>
                    <th>Projected PF Pay (₹) Per Month</th>
                </tr>
            </thead>
            <tbody>
                <% for (Map.Entry<String, PayComponents> entry : projections.entrySet()) { 
                    PayComponents pay = entry.getValue();
                %>
                    <tr>
                        <td><%= entry.getKey() %></td>
                        <td>₹ <%= String.format("%,.0f", pay.getBasic()) %></td>
                        <td>₹ <%= String.format("%,.0f", pay.getDa()) %></td>
                        <td>₹ <%= String.format("%,.0f", pay.getPfPay()) %></td>
                    </tr>
                <% } %>
            </tbody>
        </table>
    </div>
    <% } else { %>
        <p class="text-danger">No projection data available.</p>
    <% } %>
</div>

    <!-- PF Contribution Section -->
    <%
        Map<String, Double> yearlyOutflow = (Map<String, Double>) request.getAttribute("yearlyOutflow");
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        double netOutflow = 0;
        if (yearlyOutflow != null && !yearlyOutflow.isEmpty()) {
            netOutflow = yearlyOutflow.get(yearlyOutflow.keySet().toArray()[yearlyOutflow.size() - 1]);
        }
    %>
    <div class="mt-5">
        <h4 class="text-primary mb-3">💸 Year-wise Accumulated PF Outflow(If PF Pay exceeds ₹15,000, Contribution Outflow = (8.33% of PF Pay + 1.16% of (PF Pay − ₹15,000) − ₹1,250). If PF Pay is ₹15,000 or less, the contribution is zero. Contribution accumulated year on year )</h4>
        <%
            if (yearlyOutflow != null && !yearlyOutflow.isEmpty()) {
        %>
        <div class="table-responsive">
            <table class="table table-bordered table-hover table-striped">
                <thead class="table-light">
                    <tr>
                        <th>Year</th>
                        <th>Accumulated PF Amount (₹)</th>
                    </tr>
                </thead>
                <tbody>
                <% for (Map.Entry<String, Double> entry : yearlyOutflow.entrySet()) { %>
                    <tr>
                        <td><%= entry.getKey() %></td>
                        <td>₹ <%= nf.format(entry.getValue()) %></td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </div>
        
        <div class="alert alert-info fw-bold fs-5 mt-3">
            🧮 <strong>Approximate Company Contrubution PF Outflow to be paid to EPFO - From Present Year to completion of your 58 years of age :</strong> ₹ <%= (long) netOutflow %>
            <p>🧮 <strong>Please Note that initial Demand amount claimed by EPFO is not included in this amount. Hence, you need to account for that value separately. </strong> 
        </div>
        <% } else { %>
            <p class="text-danger">PF contribution details not available.</p>
        <% } %>
    </div>
    
    
    <!-- PF Contribution Section -->
<div class="mt-5">
    <h4 class="text-primary mb-3">💸 Year-wise Accumulated PF Outflow(If PF Pay exceeds ₹15,000, Contribution Outflow = (8.33% of PF Pay + 1.16% of (PF Pay − ₹15,000) − ₹1,250). If PF Pay is ₹15,000 or less, the contribution is zero. Contribution accumulated year on year )</h4>
    <%
        // Reuse existing yearlyOutflow, nf, and calculate netOutflow differently
        if (yearlyOutflow != null && !yearlyOutflow.isEmpty()) {
            // Reset netOutflow value
            netOutflow = 0;
            
            // Use retirement value if available, otherwise use the last year value
            if (yearlyOutflow.containsKey("retirement")) {
                netOutflow = yearlyOutflow.get("retirement");
            } else {
                // Get the last regular yearly entry
                String lastKey = null;
                for (String key : yearlyOutflow.keySet()) {
                    if (!"retirement".equals(key)) {
                        lastKey = key;
                    }
                }
                if (lastKey != null) {
                    netOutflow = yearlyOutflow.get(lastKey);
                }
            }
    %>
    <div class="table-responsive">
        <table class="table table-bordered table-hover table-striped">
            <thead class="table-light">
                <tr>
                    <th>Year</th>
                    <th>Accumulated PF Amount (₹)</th>
                </tr>
            </thead>
            <tbody>
            <% 
               // Display regular yearly entries
               for (Map.Entry<String, Double> entry : yearlyOutflow.entrySet()) { 
                   if (!"retirement".equals(entry.getKey())) {
            %>
                <tr>
                    <td><%= entry.getKey() %>-<%= Integer.parseInt(entry.getKey()) + 1 %></td>
                    <td>₹ <%= nf.format(entry.getValue()) %></td>
                </tr>
            <% 
                   }
               } 
            %>
            </tbody>
            <tfoot class="table-dark">
                <tr>
                    <th>Final Balance at Retirement (November 2035)</th>
                    <th>₹ <%= nf.format(netOutflow) %></th>
                </tr>
            </tfoot>
        </table>
    </div>
    
    <div class="alert alert-info fw-bold fs-5 mt-3">
        🧮 <strong>Approximate Company Contribution PF Outflow to be paid to EPFO - From Present Year to completion of your 58 years of age:</strong> ₹ <%= nf.format(netOutflow) %>
        <p>🧮 <strong>Please Note that initial Demand amount claimed by EPFO is not included in this amount. Hence, you need to account for that value separately.</strong></p>
    </div>
    <% } else { %>
        <p class="text-danger">PF contribution details not available.</p>
    <% } %>
</div>     

    <!-- Formula Explanation Section -->
    <div class="mt-4 p-4 bg-light rounded shadow-sm border">
        <h5 class="text-dark mb-3">🧮 Formula for Projected Average PF Pay</h5>
        <p>We calculate the projected average PF pay for the last 5 years using the following assumptions:</p>
        <ul class="list-group list-group-flush mb-3">
            <li class="list-group-item">📌 <strong>2025</strong> – No hike (used as base year)</li>
            <li class="list-group-item">📈 <strong>8% yearly hike (3% increment and 5% DA)</strong> from <strong>2026 to 2029</strong></li>
            <li class="list-group-item">💥 <strong>Pay Commision Assuming - 1.86 Multiplying Factor of Basic Salary with 2% DA to Start with</strong> on <strong>Jan 1, 2030. Please note that Pay commission is due on Jan 2026. Hence Notional fitment will be given from 1st Jan 2026 but actual implementation is expected to take place from Jan 2030</strong></li>
            <li class="list-group-item">🔄 <strong>5% yearly hike (3% increment and 2% DA)</strong> from <strong>2030-2040</strong> until the employee turns 58</li>
            <li class="list-group-item">🔄 <strong>Pay Commision Assuming - 1.4 Multiplying Factor of Basic Salary with 1% DA to Start with</strong> on <strong>Jan 1, 2040. Please note that Pay commission is due on Jan 2036. Hence Notional fitment will be given from 1st Jan 2026 but actual implementation is expected to take place from Jan 2040</strong></li>
            <li class="list-group-item">🔄 <strong>4% yearly hike (3% increment and 0.5% DA)</strong> from <strong>2040-2050</strong> until the employee turns 58</li>
            <li class="list-group-item"><strong>🚫 Promotions and other benefits not considered<strong></li>
        </ul>
        <p class="mb-0">Then we take the average of the projected PF pays from the last 5 years before retirement.</p>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
