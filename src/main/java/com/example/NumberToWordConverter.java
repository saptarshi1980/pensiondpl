package com.example;



public class NumberToWordConverter {
    public  final String[] units = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", 
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", 
        "Seventeen", "Eighteen", "Nineteen"
    };

    public  final String[] tens = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public  String convert(long n) {
        if (n == 0) return "Zero";
        return convertToWords(n).trim() + " INR";
    }

    public String convertToWords(long n) {
        if (n < 20) return units[(int) n];
        if (n < 100) return tens[(int) (n / 10)] + " " + units[(int) (n % 10)];
        if (n < 1000) return units[(int) (n / 100)] + " Hundred " + convertToWords(n % 100);
        if (n < 100000) return convertToWords(n / 1000) + " Thousand " + convertToWords(n % 1000);
        if (n < 10000000) return convertToWords(n / 100000) + " Lakh " + convertToWords(n % 100000);
        return convertToWords(n / 10000000) + " Crore " + convertToWords(n % 10000000);
    }
}
	
