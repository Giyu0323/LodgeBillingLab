package com.mcs.hotel.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Everything the Analytics page shows for one reporting period.
 * A figure that is a Double (not a double) can be NULL, which means "unavailable":
 * the system does not have enough information to work it out, and it never
 * invents a number instead. The "...Change" figures are the change compared
 * with the same dates earlier (a percentage, or percentage points for the margin).
 */
public record AnalyticsReport(
        AnalyticsPeriod period,
        LocalDate start,
        LocalDate end,

        // Money, always known (zero when nothing was recorded)
        double revenue,
        double roomRevenue,
        double extrasRevenue,
        double expenses,
        double netProfit,

        // Percentages that may be unavailable
        Double profitMargin,
        Double revenueChange,
        Double expensesChange,
        Double profitChange,
        Double marginChange,

        // Key performance indicators that may be unavailable
        Double roi,
        Double occupancyRate,
        Double averageDailyRate,
        Double revPar,

        // The counts behind the figures
        int billCount,
        int roomNightsSold,
        int roomNightsAvailable,

        // One row per month in the period, oldest first (for the charts and the table)
        List<MonthFigures> months) {

    /** One month's figures. The margin is null when that month had no revenue. */
    public record MonthFigures(YearMonth month, double revenue, double expenses,
                               double netProfit, Double margin) {}
}