<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pension Break-Even Calculator</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f4f4f9;
            margin: 0;
            padding: 0;
        }
        h2 {
            text-align: center;
            margin-top: 20px;
            color: #333;
        }
        .container {
            width: 50%;
            margin: 0 auto;
            padding: 20px;
            background-color: white;
            border-radius: 8px;
            box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
        }
        table {
            width: 100%;
            margin: 20px 0;
            border-collapse: collapse;
        }
        th, td {
            padding: 10px;
            text-align: left;
            border: 1px solid #ddd;
        }
        th {
            background-color: #4CAF50;
            color: white;
        }
        td input[type="text"] {
            width: 100%;
            padding: 8px;
            margin: 5px 0;
            border-radius: 4px;
            border: 1px solid #ddd;
        }
        input[type="submit"] {
            background-color: #4CAF50;
            color: white;
            padding: 10px 15px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            width: 100%;
            font-size: 16px;
        }
        input[type="submit"]:hover {
            background-color: #45a049;
        }
    </style>
</head>
<body>
<div style="padding: 10px; background-color: #e9ecef; text-align: center;">
  <a href="/pensiondpl/index.jsp" font-size: 18px; color: #333; font-weight: bold;">
    Pension Calculator
  </a>
    <h2>Pension Break-Even Calculator</h2>
    <div class="container">
        <form action="PensionBreakEvenServlet" method="post">
            <table>
                <tr>
                    <th>Lump Sum Amount</th>
                    <td><input type="text" name="lumpSum"></td>
                </tr>
                <tr>
                    <th>Monthly Pension</th>
                    <td><input type="text" name="monthlyPension"></td>
                </tr>
                <tr>
                    <th>Annual FD Interest Rate (%)</th>
                    <td><input type="text" name="fdRate"></td>
                </tr>
                <tr>
                    <th>Annual RD Interest Rate (%)</th>
                    <td><input type="text" name="rdRate"></td>
                </tr>
            </table>
            <input type="submit" value="Calculate">
        </form>
    </div>
</body>
</html>
