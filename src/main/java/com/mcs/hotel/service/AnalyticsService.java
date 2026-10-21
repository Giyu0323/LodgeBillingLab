package com.mcs.hotel.service;

import com.mcs.hotel.model.AnalyticsPeriod;
import com.mcs.hotel.model.AnalyticsReport;
import com.mcs.hotel.model.Bill;
import com.mcs.hotel.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Works out the Business Analytics figures from the real bills and expenses.
 * It only READS other services' data. A figure it cannot work out honestly is
 * returned as null ("unavailable"), never as an invented number.
 */
public class AnalyticsService {

    private final BillingService billing;
    private final ExpenseService expenses;
    private final RoomService rooms;

    /** The money invested in the lodge, entered by the owner. 0 means "not entered yet". */
    private double investedCapital = 0;

    public AnalyticsService(BillingService billing, ExpenseService expenses, RoomService rooms) {
        this.billing = billing;
        this.expenses = expenses;
        this.rooms = rooms;
    }

    // ---------- Invested capital (needed only for ROI) ----------

    public double getInvestedCapital() {
        return investedCapital;
    }

    public boolean hasInvestedCapital() {
        return investedCapital > 0;
    }

    /** Sets the invested capital. Zero clears it (ROI becomes unavailable again). */
    public void setInvestedCapital(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Invested capital cannot be negative.");
        }
        if (BigDecimal.valueOf(amount).stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("The amount can have at most 2 decimal places.");
        }
        this.investedCapital = amount;
    }

    // ---------- The report ----------

    public AnalyticsReport report(AnalyticsPeriod period) {
        return report(period, LocalDate.now());
    }

    /** The same, for a chosen "today" (so the figures can be tested for any date). */
    public AnalyticsReport report(AnalyticsPeriod period, LocalDate today) {
        List<Bill> bills = billing.getBills();
        List<Expense> spent = expenses.getAll();

        AnalyticsPeriod.Range range = period.range(today, earliestDate(bills, spent));
        Figures now = figures(range, bills, spent);

        // The earlier period to compare with (null when there is none, e.g. All Time)
        Figures before = period.previousRange(range)
                .map(r -> figures(r, bills, spent))
                .orElse(null);

        double profit = now.revenue() - now.expenses();
        Double margin = marginOf(now.revenue(), profit);

        Double revenueChange = null;
        Double expensesChange = null;
        Double profitChange = null;
        Double marginChange = null;
        if (before != null) {
            double beforeProfit = before.revenue() - before.expenses();
            revenueChange = percentChange(now.revenue(), before.revenue());
            expensesChange = percentChange(now.expenses(), before.expenses());
            profitChange = percentChange(profit, beforeProfit);
            Double beforeMargin = marginOf(before.revenue(), beforeProfit);
            if (margin != null && beforeMargin != null) {
                marginChange = margin - beforeMargin; // percentage points
            }
        }

        // ---- Occupancy, ADR and RevPAR ----
        int available = rooms.totalCount() * (int) range.days();
        int sold = nightsSoldIn(range, bills);

        Double occupancy = available > 0 ? Double.valueOf(sold * 100.0 / available) : null;
        Double adr = now.nightsBilled() > 0 ? Double.valueOf(now.roomRevenue() / now.nightsBilled()) : null;
        Double revPar = available > 0 ? Double.valueOf(now.roomRevenue() / available) : null;
        Double roi = hasInvestedCapital() ? Double.valueOf(profit * 100 / investedCapital) : null;

        return new AnalyticsReport(
                period, range.start(), range.end(),
                now.revenue(), now.roomRevenue(), now.extrasRevenue(), now.expenses(), profit,
                margin, revenueChange, expensesChange, profitChange, marginChange,
                roi, occupancy, adr, revPar,
                now.billCount(), sold, available,
                monthlyFigures(range, bills, spent));
    }

    // ---------- Building blocks ----------

    /** The totals for the bills that checked in, and the expenses dated, inside a range. */
    private record Figures(double revenue, double roomRevenue, double extrasRevenue,
                           double expenses, int billCount, int nightsBilled) {}

    private Figures figures(AnalyticsPeriod.Range range, List<Bill> bills, List<Expense> spent) {
        double revenue = 0;
        double room = 0;
        double extras = 0;
        int count = 0;
        int nights = 0;
        for (Bill bill : bills) {
            if (within(bill.checkInDate(), range)) {
                revenue += bill.total();
                room += bill.roomCharge() - bill.discount(); // room revenue is after the discount
                extras += bill.extrasTotal();
                count++;
                nights += bill.nights();
            }
        }
        double expenseTotal = spent.stream()
                .filter(e -> within(e.date(), range))
                .mapToDouble(Expense::amount)
                .sum();
        return new Figures(revenue, room, extras, expenseTotal, count, nights);
    }

    /** One entry per month in the range, oldest first. A part-month is clipped to the range. */
    private List<AnalyticsReport.MonthFigures> monthlyFigures(AnalyticsPeriod.Range range,
                                                              List<Bill> bills, List<Expense> spent) {
        List<AnalyticsReport.MonthFigures> months = new ArrayList<>();
        YearMonth month = YearMonth.from(range.start());
        YearMonth last = YearMonth.from(range.end());
        while (!month.isAfter(last)) {
            LocalDate from = later(range.start(), month.atDay(1));
            LocalDate to = earlier(range.end(), month.atEndOfMonth());
            Figures f = figures(new AnalyticsPeriod.Range(from, to), bills, spent);
            double profit = f.revenue() - f.expenses();
            months.add(new AnalyticsReport.MonthFigures(month, f.revenue(), f.expenses(),
                    profit, marginOf(f.revenue(), profit)));
            month = month.plusMonths(1);
        }
        return months;
    }

    /**
     * Room-nights actually sold inside the range. Each stay runs from its check-in
     * day up to (not including) its last night's morning; a guest who checked out
     * early only counts the nights they stayed (at least one).
     */
    private int nightsSoldIn(AnalyticsPeriod.Range range, List<Bill> bills) {
        int sold = 0;
        LocalDate rangeEndExclusive = range.end().plusDays(1);
        for (Bill bill : bills) {
            LocalDate stayStart = bill.checkInDate();
            LocalDate stayEnd = stayStart.plusDays(bill.nights()); // the day they leave

            if (bill.isCheckedOut()) {
                LocalDate left = bill.checkedOutAt().toLocalDate();
                LocalDate leftEnd = left.isAfter(stayStart) ? left : stayStart.plusDays(1);
                if (leftEnd.isBefore(stayEnd)) {
                    stayEnd = leftEnd;
                }
            }

            LocalDate from = later(stayStart, range.start());
            LocalDate to = earlier(stayEnd, rangeEndExclusive);
            if (to.isAfter(from)) {
                sold += (int) ChronoUnit.DAYS.between(from, to);
            }
        }
        return sold;
    }

    private LocalDate earliestDate(List<Bill> bills, List<Expense> spent) {
        LocalDate earliest = null;
        for (Bill bill : bills) {
            if (earliest == null || bill.checkInDate().isBefore(earliest)) {
                earliest = bill.checkInDate();
            }
        }
        for (Expense expense : spent) {
            if (earliest == null || expense.date().isBefore(earliest)) {
                earliest = expense.date();
            }
        }
        return earliest;
    }

    // ---------- Small helpers ----------

    private boolean within(LocalDate date, AnalyticsPeriod.Range range) {
        return !date.isBefore(range.start()) && !date.isAfter(range.end());
    }

    private LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDate earlier(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /** Profit as a percentage of revenue, or null when there is no revenue to compare with. */
    private Double marginOf(double revenue, double profit) {
        return revenue > 0 ? Double.valueOf(profit * 100 / revenue) : null;
    }

    /** How much "now" differs from "before", as a percentage. Null when "before" was zero. */
    private Double percentChange(double now, double before) {
        if (before == 0) {
            return null;
        }
        return (now - before) / Math.abs(before) * 100;
    }
}