package com.hrms.project.utility;

import java.math.BigDecimal;

import java.math.RoundingMode;

public class OvertimeCalculator {

    private static final double FIRST_SLAB_HOURS = 2.0;

    public static BigDecimal calculateAmount(BigDecimal dailyWageRate, double overtimeHours) {

        if (overtimeHours <= 0) {
            return BigDecimal.ZERO;
        }


        BigDecimal hourlyRate = dailyWageRate
                .divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);


        double firstHours = Math.min(overtimeHours, FIRST_SLAB_HOURS);


        double remainingHours = Math.max(0.0, overtimeHours - FIRST_SLAB_HOURS);


        BigDecimal firstAmount = hourlyRate
                .multiply(BigDecimal.valueOf(1.5))
                .multiply(BigDecimal.valueOf(firstHours));

        BigDecimal remainingAmount = hourlyRate
                .multiply(BigDecimal.valueOf(2.0))
                .multiply(BigDecimal.valueOf(remainingHours));


        return firstAmount.add(remainingAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
