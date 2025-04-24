package com.example;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PensionService {

    public double calculatePension(double pensionableServiceDays, 
                                   double highestSalaryTill2014,
                                   LocalDate exitDate,
                                   double averageSalaryLast5Years) {

        double serviceWithAdditionalDays = pensionableServiceDays + (2*365);
        long serviceDays = (long) (serviceWithAdditionalDays);
        double firstPart = serviceDays * highestSalaryTill2014;

        LocalDate startDate = LocalDate.of(2014, 9, 1);
        long daysAfter2014 = ChronoUnit.DAYS.between(startDate, exitDate);
        
        double secondPart = daysAfter2014 * averageSalaryLast5Years;
        
        //This is if first part is not considered as per Court's judgement
        //double secondPart = (daysAfter2014+(2*365)) * averageSalaryLast5Years;
        //firstPart=0;

        return (firstPart + secondPart) / (70 * 365);
    }
}
