<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Set" %>
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
            <div class="detail-label">Retirement Month-End:</div>
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
            Map<String, Integer> projections = (Map<String, Integer>) request.getAttribute("yearlyProjections");
            if (projections != null && !projections.isEmpty()) {
        %>
        <div class="table-responsive">
            <table class="table table-striped table-bordered table-hover">
                <thead class="table-light">
                    <tr>
                        <th>End of Year</th>
                        <th>Projected PF Pay (₹)</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (Map.Entry<String, Integer> entry : projections.entrySet()) { %>
                        <tr>
                            <td><%= entry.getKey() %></td>
                            <td>₹ <%= entry.getValue() %></td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
        <% } else { %>
            <p class="text-danger">No projection data available.</p>
        <% } %>
    </div>

    <!-- Formula Explanation Section -->
    <div class="mt-4 p-4 bg-light rounded shadow-sm border">
        <h5 class="text-dark mb-3">🧮 Formula for Projected Average PF Pay</h5>
        <p>We calculate the projected average PF pay for the last 5 years using the following assumptions:</p>
        <ul class="list-group list-group-flush mb-3">
            <li class="list-group-item">📌 <strong>2025</strong> – No hike (used as base year)</li>
            <li class="list-group-item">📈 <strong>8% yearly hike</strong> from <strong>2026 to 2028</strong></li>
            <li class="list-group-item">💥 <strong>1.5x hike</strong> on <strong>Jan 1, 2029</strong> (Pay Commission revision)</li>
            <li class="list-group-item">🔄 <strong>4% yearly hike</strong> from <strong>2030 onward</strong> until the employee turns 58</li>
            <li class="list-group-item">🚫 Promotions and other benefits not considered</li>
        </ul>
        <p class="mb-0">Then we take the average of the projected PF pays from the last 5 years before retirement.</p>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
