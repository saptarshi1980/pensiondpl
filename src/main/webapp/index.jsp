<!DOCTYPE html>
<html>
<head>
    <title>Pension Calculator</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container">
        <h2>Pension Calculator</h2>
        <form action="FetchEmployeeServlet" method="post">
            <label for="empId">Enter Employee ID</label>
            <input type="text" name="empId" id="empId" required placeholder="Employee ID">
            <button type="submit">Next</button>
        </form>
    </div>
</body>
</html>
