<!DOCTYPE html>
<html>
<head>
<title>projection for demand till April 2025</title>
<!-- Bootstrap CSS -->
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
	rel="stylesheet">
<style>
.disclaimer-box {
	border-left: 5px solid #ffc107;
	background-color: #fff3cd;
}
.word-value {
    font-size: 0.9rem;
    color: #28a745;
    font-weight: bold;
    margin-top: 5px;
}
</style>
<link rel="stylesheet"
	href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
</head>
<body>
	<div class="container-fluid p-3 bg-light">
		<div class="text-center mb-3">
			<a href="/pensiondpl/index.jsp"
				class="btn btn-link fs-5 fw-bold text-primary text-decoration-none">
				Click here for Pension Calculator </a>
		</div>

	<div class="container col-md-8 mx-auto" align="center">  <!-- Increased from col-md-6 to col-md-8 -->
    <h2 class="text-center mb-3 p-2 text-white rounded-3"
        style="background: linear-gradient(135deg, #3a7bd5, #00d2ff); 
               box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
               font-weight: 500;
               letter-spacing: 0.1px;
               font-size: 1.25rem;  <!-- Slightly smaller font -->
               white-space: nowrap; <!-- Prevent line breaks -->
               overflow: hidden;
               text-overflow: ellipsis;">
        <i class="fas fa-calculator me-2" ></i> EPFO Demand Projection till April 2025
    </h2>
</div>

			<!-- Disclaimer Box -->
			<!-- Disclaimer Box -->
<div class="disclaimer-box p-3 mb-4 rounded-end">
  <h5 class="fw-bold">Disclaimer:</h5>
  <p>EPFO issued Demand letter with a specific Value</p>
  <ul class="mb-2">
    <li>Although this amount is payable in June 2025 but the amount is calculated till March 2023</li>
    <li>You have to make a projection of Demand value till 2025 based on your contribution and accumulated interest </li>
  </ul>

</div>

	
			<form action="FetchEmployeeServlet23To251" method="post"
				class="needs-validation" novalidate>
				<div class="mb-3">
					<label for="empId" class="form-label"><strong>Enter Employee ID</strong></label> <input
						type="text" class="form-control" name="empId" id="empId" required
						pattern="\d{4,5}" placeholder="Employee ID">
					<div class="invalid-feedback">Please enter a valid Employee
						ID .</div>
						
					<label for="demand_amt" class="form-label"><strong>Enter EPFO Demand Amount</strong></label> 
					<input type="text" class="form-control" name="demand_amt" id="demand_amt" required
						pattern="\d{}" placeholder="Enter EPFO Demand Amount">
					<div id="wordValue" class="word-value"></div>
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
		// Function to convert number to words
		function numberToWords(num) {
			const ones = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine'];
			const teens = ['Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen'];
			const tens = ['', 'Ten', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];
			
			if (num === 0) return 'Zero';
			
			function convertLessThanOneThousand(num) {
				if (num === 0) return '';
				if (num < 10) return ones[num];
				if (num < 20) return teens[num - 10];
				if (num < 100) return tens[Math.floor(num / 10)] + (num % 10 !== 0 ? ' ' + ones[num % 10] : '');
				return ones[Math.floor(num / 100)] + ' Hundred' + (num % 100 !== 0 ? ' ' + convertLessThanOneThousand(num % 100) : '');
			}
			
			let result = '';
			const scales = ['', 'Thousand', 'Lakh', 'Crore'];
			let scaleIndex = 0;
			
			while (num > 0) {
				let chunk;
				if (scaleIndex === 0) {
					// For the first scale (units), take 3 digits
					chunk = num % 1000;
					num = Math.floor(num / 1000);
					scaleIndex++;
				} else {
					// For Indian numbering system (Lakhs and Crores), take 2 digits
					chunk = num % 100;
					num = Math.floor(num / 100);
					scaleIndex++;
				}
				
				if (chunk !== 0) {
					let chunkStr = convertLessThanOneThousand(chunk);
					if (scaleIndex > 1) {
						chunkStr += ' ' + scales[scaleIndex - 1];
					}
					result = chunkStr + ' ' + result;
				}
			}
			
			return result.trim() + ' Rupees';
		}
		
		// Event listener for demand_amt input
		document.getElementById('demand_amt').addEventListener('input', function() {
			const value = this.value.replace(/[^0-9]/g, ''); // Remove non-numeric characters
			const numValue = value === '' ? 0 : parseInt(value, 10);
			
			if (!isNaN(numValue)) {
				// Convert to words and display
				const wordValue = numberToWords(numValue);
				document.getElementById('wordValue').textContent = wordValue;
			}
		});
		
		// Enable/disable submit button based on checkbox
		document.getElementById('agreeCheckbox').addEventListener('change', function() {
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