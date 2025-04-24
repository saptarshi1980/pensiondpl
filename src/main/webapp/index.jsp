<!DOCTYPE html>
<html>
<head>
<title>Pension Calculator</title>
<!-- Bootstrap CSS -->
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
	rel="stylesheet">
<style>
.disclaimer-box {
	border-left: 5px solid #ffc107;
	background-color: #fff3cd;
}
</style>
<link rel="stylesheet"
	href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
</head>
<body>
	<div class="container-fluid p-3 bg-light">
		<div class="text-center mb-3">
			<a href="/pensiondpl/break-even.jsp"
				class="btn btn-link fs-5 fw-bold text-primary text-decoration-none">
				Click here for Pension Break-Even Period Calculator </a>
		</div>

	<div class="container col-md-8 mx-auto">  <!-- Increased from col-md-6 to col-md-8 -->
    <h2 class="text-center mb-3 p-2 text-white rounded-3"
        style="background: linear-gradient(135deg, #3a7bd5, #00d2ff); 
               box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
               font-weight: 500;
               letter-spacing: 0.1px;
               font-size: 1.25rem;  <!-- Slightly smaller font -->
               white-space: nowrap; <!-- Prevent line breaks -->
               overflow: hidden;
               text-overflow: ellipsis;">
        <i class="fas fa-calculator me-2"></i> Pension on Higher Wages and Company's side PF Contribution outflow Calculator
    </h2>
</div>

			<!-- Disclaimer Box -->
			<div class="disclaimer-box p-3 mb-4 rounded-end">
				<h5 class="fw-bold">Disclaimer:</h5>
				<p>This Projection calculator is based on many future assumption like:</p>
				<ul class="mb-2">
					<li>Increment and fixed DA will be given each year</li>
					<li>7th Pay commission will be effective from 2026 and
						implemented in 2030</li>
					<li>8th Pay commission will be effective from 2036 and
						implemented in 2040</li>
					<li>Promotions/upliftment not considered</li>
				</ul>
				<p class="mb-0">Therefore, The projected Pension and Company
					side PF outflow amounts are totally approximate in nature based on the formula
					given by EPFO.The company side PF Outflow (From current date
					to retirement-58years) which will be calculated by this system does
					not include one time demand amount of EPFO.</p>
			</div>

			<form action="FetchEmployeeServlet" method="post"
				class="needs-validation" novalidate>
				<div class="mb-3">
					<label for="empId" class="form-label">Enter Employee ID</label> <input
						type="text" class="form-control" name="empId" id="empId" required
						pattern="\d{4,5}" placeholder="Employee ID">
					<div class="invalid-feedback">Please enter a valid Employee
						ID .</div>
				</div>

				<div class="form-check mb-3">
					<input class="form-check-input" type="checkbox" id="agreeCheckbox"
						required> <label class="form-check-label"
						for="agreeCheckbox"> I agree to the above disclaimer </label>
					<div class="invalid-feedback">You must agree before
						submitting.</div>
				</div>

				<div class="d-grid">
					<button type="submit" class="btn btn-primary" id="submitButton"
						disabled>Next</button>
				</div>
			</form>
		</div>
	</div>

	<!-- Bootstrap JS Bundle with Popper -->
	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
	<script>
		// Enable/disable submit button based on checkbox
		document
				.getElementById('agreeCheckbox')
				.addEventListener(
						'change',
						function() {
							document.getElementById('submitButton').disabled = !this.checked;
						});

		// Additional validation for employee ID
		document.getElementById('empId').addEventListener('input', function() {
			this.value = this.value.replace(/[^0-9]/g, ''); // Remove non-numeric characters
		});

		// Bootstrap form validation
		(function() {
			'use strict';
			var forms = document.querySelectorAll('.needs-validation');
			Array.prototype.slice.call(forms).forEach(function(form) {
				form.addEventListener('submit', function(event) {
					if (!form.checkValidity()) {
						event.preventDefault();
						event.stopPropagation();
					}
					form.classList.add('was-validated');
				}, false);
			});
		})();
	</script>
</body>
</html>