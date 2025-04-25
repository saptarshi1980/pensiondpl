package com.example;

public class PFMonthlyCalculation {
    private final double basicSalary;
    private final double dearnessAllowance;
    private final double pfPay;
    private final double contribution;
    private final double monthlyInterest;
    private final double openingBalance;
    private final double closingBalance;
    private final double accruedInterest;
    private final double closingBalanceWithInterest;
    
    public PFMonthlyCalculation(double basicSalary, double dearnessAllowance, double pfPay, 
            double contribution, double monthlyInterest, double openingBalance, 
            double closingBalance, double accruedInterest, double closingBalanceWithInterest) {
        this.basicSalary = basicSalary;
        this.dearnessAllowance = dearnessAllowance;
        this.pfPay = pfPay;
        this.contribution = contribution;
        this.monthlyInterest = monthlyInterest;
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
        this.accruedInterest = accruedInterest;
        this.closingBalanceWithInterest = closingBalanceWithInterest;
    }
    
    public double getBasicSalary() {
        return basicSalary;
    }
    
    public double getDearnessAllowance() {
        return dearnessAllowance;
    }
    
    public double getPfPay() {
        return pfPay;
    }
    
    public double getContribution() {
        return contribution;
    }
    
    public double getMonthlyInterest() {
        return monthlyInterest;
    }
    
    public double getOpeningBalance() {
        return openingBalance;
    }
    
    public double getClosingBalance() {
        return closingBalance;
    }
    
    public double getAccruedInterest() {
        return accruedInterest;
    }
    
    public double getClosingBalanceWithInterest() {
        return closingBalanceWithInterest;
    }
}
