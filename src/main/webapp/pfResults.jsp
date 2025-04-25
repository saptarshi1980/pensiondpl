<%@ page contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry" %>
<%@ page import="com.example.PFMonthlyCalculation" %>
<%@ page import="java.text.DecimalFormat" %>
<%
    Map<String, PFMonthlyCalculation> results = (Map<String, PFMonthlyCalculation>) request.getAttribute("pfResults");
    DecimalFormat df = new DecimalFormat("#,##0.00");
    String empName = (String) request.getAttribute("empName");
    
    // Calculate summary information
    double totalContributions = 0;
    double totalInterest = 0;
    double finalClosingBalance = 0;
    double finalClosingBalanceWithInterest = 0;
    double oneMonthExtraInterest = 0;
    
    if (results != null && !results.isEmpty()) {
        // Find the last entry for final balance
        Entry<String, PFMonthlyCalculation> lastEntry = null;
        for (Entry<String, PFMonthlyCalculation> entry : results.entrySet()) {
            totalContributions += entry.getValue().getContribution();
            totalInterest += entry.getValue().getMonthlyInterest();
            lastEntry = entry;
        }
        
        if (lastEntry != null) {
            PFMonthlyCalculation lastCalc = lastEntry.getValue();
            finalClosingBalance = lastCalc.getClosingBalance();
            
            // Calculate one additional month's interest (8.25% yearly / 12)
            oneMonthExtraInterest = finalClosingBalance * 0.0825 / 12;
            finalClosingBalanceWithInterest = finalClosingBalance + oneMonthExtraInterest;
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="ISO-8859-1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PF Projection Results</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Font Awesome -->
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <!-- DataTables CSS -->
    <link href="https://cdn.datatables.net/1.13.1/css/dataTables.bootstrap5.min.css" rel="stylesheet">
    <style>
        body {
            padding: 20px;
            background-color: #f8f9fa;
        }
        .card {
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
            margin-bottom: 30px;
        }
        .card-header {
            background-color: #f1f8ff;
            font-weight: bold;
        }
        .table-responsive {
            overflow-x: auto;
        }
        .summary-row {
            background-color: #e8f4f8;
            font-weight: bold;
        }
        .final-row {
            background-color: #d1e7dd;
            font-weight: bold;
            font-size: 1.1rem;
        }
        .table th {
            background-color: #0d6efd;
            color: white;
        }
        .contributions-box {
            background-color: #e7f5ff;
        }
        .interest-box {
            background-color: #fff4e6;
        }
        .total-box {
            background-color: #f8f9d7;
        }
        .projection-box {
            background-color: #d7f9e9;
        }
    </style>
</head>
<body>
    <div class="container">
        <!-- Centered Gradient Header as Requested -->
        <div class="row mb-4">
            <div class="col-12 text-center">
                <h2 class="text-center mb-3 p-2 text-white rounded-3"
                    style="background: linear-gradient(135deg, #3a7bd5, #00d2ff); 
                           box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                           font-weight: 500;
                           letter-spacing: 0.1px;
                           font-size: 1.25rem;">
                    <i class="fas fa-calculator me-2"></i> EPFO Demand Projection From April 2023 to April 2025/ Retirement(whichever is earlier) - <%= empName != null ? empName : "" %>
                </h2>
            </div>
        </div>

        <div class="row mb-4">
            <div class="col">
                <div class="card">
                    <div class="card-body">
                        <div class="table-responsive">
                            <table id="pfTable" class="table table-striped table-hover table-bordered">
                                <thead>
                                    <tr class="bg-primary">
                                        <th>Month</th>
                                        <th>Basic</th>
                                        <th>DA</th>
                                        <th>PF Pay</th>
                                        <th>Contribution</th>
                                        <th>Interest</th>
                                        <th>Opening Balance</th>
                                        <th>Accrued Interest</th>
                                        <th>Closing Balance</th>
                                    </tr>
                                </thead>
                                <tbody>
                                <%
                                    if (results != null && !results.isEmpty()) {
                                        for (Map.Entry<String, PFMonthlyCalculation> entry : results.entrySet()) {
                                            PFMonthlyCalculation calc = entry.getValue();
                                %>
                                    <tr>
                                        <td><%= entry.getKey() %></td>
                                        <td class="text-end"><%= df.format(calc.getBasicSalary()) %></td>
                                        <td class="text-end"><%= df.format(calc.getDearnessAllowance()) %></td>
                                        <td class="text-end"><%= df.format(calc.getPfPay()) %></td>
                                        <td class="text-end"><%= df.format(calc.getContribution()) %></td>
                                        <td class="text-end"><%= df.format(calc.getMonthlyInterest()) %></td>
                                        <td class="text-end"><%= df.format(calc.getOpeningBalance()) %></td>
                                        <td class="text-end"><%= df.format(calc.getAccruedInterest()) %></td>
                                        <td class="text-end"><%= df.format(calc.getClosingBalance()) %></td>
                                    </tr>
                                <%
                                        }
                                    } else {
                                %>
                                    <tr><td colspan="9" class="text-center">No data found</td></tr>
                                <%
                                    }
                                %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-md-8 mx-auto">
                <div class="card">
                    <div class="card-header bg-success text-white text-center">
                        <h3 class="mb-0">Summary</h3>
                    </div>
                    <div class="card-body">
                        <table class="table table-bordered">
                            <thead>
                                <tr class="bg-success text-white">
                                    <th>Description</th>
                                    <th class="text-end">Amount</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr class="contributions-box">
                                    <td>Total Contributions (April 2023 to April 2025/Retirement)</td>
                                    <td class="text-end">Rs. <%= df.format(totalContributions) %></td>
                                </tr>
                                <tr class="interest-box">
                                    <td>Total Interest Accrued</td>
                                    <td class="text-end">Rs. <%= df.format(totalInterest) %></td>
                                </tr>
                                <tr class="total-box">
                                    <td>Total</td>
                                    <td class="text-end">Rs. <%= df.format(finalClosingBalance) %></td>
                                </tr>
                                <tr>
                                    <td>Interest for one month for above total</td>
                                    <td class="text-end">Rs. <%= df.format(oneMonthExtraInterest) %></td>
                                </tr>
                                <tr class="final-row projection-box">
                                    <td>Projected Demand value made by EPFO as on April 2025<br><small>(Input this value in pension calculator's EPFO Demand field)</small></td>
                                    <td class="text-end">Rs. <%= df.format(finalClosingBalanceWithInterest) %></td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="text-center mb-3">
			<a href="/pensiondpl/index.jsp"
				class="btn btn-link fs-5 fw-bold text-primary text-decoration-none">
				Click here for Pension Calculator </a>
		</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Bootstrap Bundle with Popper -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/js/bootstrap.bundle.min.js"></script>
    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.6.3.min.js"></script>
    <!-- DataTables -->
    <script src="https://cdn.datatables.net/1.13.1/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.1/js/dataTables.bootstrap5.min.js"></script>
    <script>
        $(document).ready(function() {
            $('#pfTable').DataTable({
                "pageLength": 12,
                "lengthMenu": [[10, 12, 24, -1], [10, 12, 24, "All"]],
                "order": [], // Disable initial sorting
                "language": {
                    "search": "Filter results:",
                    "lengthMenu": "Show _MENU_ months"
                },
                "dom": '<"top"fl>rt<"bottom"ip><"clear">'
            });
        });
    </script>
</body>
</html>