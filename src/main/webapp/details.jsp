<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.text.NumberFormat" %>
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
        .highlight-box {
            background-color: #fff8e1;
            border-left: 4px solid #ffc107;
            padding: 15px;
            margin: 20px 0;
        }
        .hike-badge {
            font-size: 0.8rem;
            padding: 3px 6px;
            border-radius: 4px;
            background-color: #e3f2fd;
            color: #1565c0;
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

    <!-- Year-wise Projection Section -->
    <div class="mt-5">
        <h4 class="text-primary mb-3">📅 Year-wise Projected PF Pay - Entirely Hypothetical</h4>
        <%
            Map<String, Integer> projections = (LinkedHashMap<String, Integer>) request.getAttribute("yearlyProjections");
            if (projections != null && !projections.isEmpty()) {
        %>
        <div class="table-responsive">
            <table class="table table-striped table-bordered table-hover">
                <thead class="table-light">
                    <tr>
                        <th>End of Year</th>
                        <th>Projected PF Pay (₹)</th>
                        <th>Hike Applied</th>
                    </tr>
                </thead>
                <tbody>
                    <%
                    int prevYear = 0;
                    double prevPf = 0;
                    for (Map.Entry<String, Integer> entry : projections.entrySet()) {
                        int year = Integer.parseInt(entry.getKey());
                        double currentPf = entry.getValue();
                        String hikeDescription = "";
                        
                        if (prevYear != 0) {
                            if (year == 2030) {
                                hikeDescription = "<span class='hike-badge'>1.5x Pay Commission</span>";
                            } else if (year < 2030) {
                                if (year == 2025) {
                                    hikeDescription = "<span class='hike-badge'>Base Year</span>";
                                } else {
                                    hikeDescription = "<span class='hike-badge'>5% DA + 3% Increment</span>";
                                }
                            } else {
                                hikeDescription = "<span class='hike-badge'>1% DA + 3% Increment</span>";
                            }
                        } else {
                            hikeDescription = "<span class='hike-badge'>Base Year</span>";
                        }
                        
                        prevYear = year;
                        prevPf = currentPf;
                    %>
                        <tr>
                            <td><%= entry.getKey() %></td>
                            <td>₹ <%= entry.getValue() %></td>
                            <td><%= hikeDescription %></td>
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
        Map<String, Double> yearlyOutflow = (LinkedHashMap<String, Double>) request.getAttribute("yearlyOutflow");
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        double netOutflow = 0;
        if (yearlyOutflow != null && !yearlyOutflow.isEmpty()) {
            netOutflow = yearlyOutflow.get(yearlyOutflow.keySet().toArray()[yearlyOutflow.size() - 1]);
        }
    %>
    <div class="mt-5">
        <h4 class="text-primary mb-3">💸 Year-wise Accumulated PF Outflow (9.49% of monthly PF Contribution + 8.5% annual interest)</h4>
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
            🧮 <strong>Approximate Company Contribution PF Outflow to be paid to EPFO:</strong> ₹ <%= (long) netOutflow %>
        </div>
        <% } else { %>
            <p class="text-danger">PF contribution details not available.</p>
        <% } %>
    </div>

    <!-- Updated Formula Explanation Section -->
    <div class="mt-4 p-4 bg-light rounded shadow-sm border">
        <h5 class="text-dark mb-3">🧮 PF Projection Calculation Methodology</h5>
        
        <div class="highlight-box">
            <h6 class="fw-bold">📌 Current Year (2025):</h6>
            <p>No hike applied - uses current PF pay as base value</p>
            
            <h6 class="fw-bold mt-3">📈 Until December 2029:</h6>
            <ul>
                <li><strong>5% DA hike</strong> every January</li>
                <li><strong>3% increment hike</strong> in month <%= request.getAttribute("incrementMonth") %></li>
            </ul>
            
            <h6 class="fw-bold mt-3">💥 January 2030:</h6>
            <p><strong>1.5x (50%) one-time hike</strong> for Pay Commission implementation</p>
            <p><em>Note: The 3% increment still applies in month <%= request.getAttribute("incrementMonth") %> after the 1.5x hike</em></p>
            
            <h6 class="fw-bold mt-3">🔄 From January 2031 until retirement:</h6>
            <ul>
                <li><strong>1% DA hike</strong> every January</li>
                <li><strong>Continuing 3% increment hike</strong> in month <%= request.getAttribute("incrementMonth") %></li>
            </ul>
        </div>
        
        <div class="mt-3">
            <h6 class="fw-bold">💰 PF Contribution Calculation:</h6>
            <ul>
                <li>9.49% of monthly PF pay contributed by company</li>
                <li>8.5% annual interest on accumulated balance</li>
            </ul>
            
            <h6 class="fw-bold mt-3">📊 Average Calculation:</h6>
            <p>Average of last 5 years' PF pay before retirement (60 months)</p>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>