package com.mcs.hotel;

import com.mcs.hotel.model.AnalyticsPeriod;
import com.mcs.hotel.model.AnalyticsReport;
import com.mcs.hotel.model.ExpenseCategory;
import com.mcs.hotel.model.ExtraSelection;
import com.mcs.hotel.model.RoomType;
import com.mcs.hotel.repository.InMemoryBillRepository;
import com.mcs.hotel.repository.InMemoryExpenseRepository;
import com.mcs.hotel.service.AnalyticsService;
import com.mcs.hotel.service.BillingService;
import com.mcs.hotel.service.ExpenseService;
import com.mcs.hotel.service.PriceService;
import com.mcs.hotel.service.RoomService;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** TEMPORARY test: prints the analytics report for sample records. Delete when Analytics is finished. */
public class Check {

    public static void main(String[] args) {
        PriceService prices = new PriceService();
        RoomService rooms = new RoomService();
        BillingService billing = new BillingService(new InMemoryBillRepository(), rooms, prices);
        ExpenseService expenses = new ExpenseService(new InMemoryExpenseRepository());
        AnalyticsService analytics = new AnalyticsService(billing, expenses, rooms);

        // Two bills, both checked in today. Pay Later, so no payment is needed.
        List<ExtraSelection> breakfast = List.of(new ExtraSelection(prices.getExtras().get(0), 5)); // Breakfast, 5 days
        billing.createBill("John Phiri", RoomType.DELUXE, 5, breakfast, null, ""); // total K 2,862.50
        billing.createBill("Mary Banda", RoomType.STANDARD, 1, List.of(), null, ""); // total K 350.00

        // Two expenses: one today, one the same day last month
        expenses.add(LocalDate.now(), ExpenseCategory.UTILITIES, "Electricity", 450);
        expenses.add(LocalDate.now().minusMonths(1), ExpenseCategory.STAFF, "Wages", 3000);

        show(analytics.report(AnalyticsPeriod.THIS_MONTH));
        show(analytics.report(AnalyticsPeriod.LAST_MONTH));
        show(analytics.report(AnalyticsPeriod.ALL_TIME));

        System.out.println("--- ROI before and after entering capital ---");
        System.out.println("Before: " + fmt(analytics.report(AnalyticsPeriod.THIS_MONTH).roi()));
        analytics.setInvestedCapital(100000);
        System.out.println("After K100,000: " + fmt(analytics.report(AnalyticsPeriod.THIS_MONTH).roi()));
    }

    private static void show(AnalyticsReport r) {
        System.out.println("=== " + r.period() + "  (" + r.start() + " to " + r.end() + ") ===");
        System.out.printf(Locale.US, "Revenue %.2f | Room %.2f | Extras %.2f | Expenses %.2f | Profit %.2f%n",
                r.revenue(), r.roomRevenue(), r.extrasRevenue(), r.expenses(), r.netProfit());
        System.out.println("Margin " + fmt(r.profitMargin()) + " | ADR " + fmt(r.averageDailyRate())
                + " | RevPAR " + fmt(r.revPar()));
        System.out.println("Occupancy " + fmt(r.occupancyRate()) + "  (" + r.roomNightsSold()
                + " of " + r.roomNightsAvailable() + " room-nights)");
        System.out.println("Change: revenue " + fmt(r.revenueChange()) + " | expenses " + fmt(r.expensesChange())
                + " | profit " + fmt(r.profitChange()) + " | margin " + fmt(r.marginChange()));
        r.months().forEach(m -> System.out.printf(Locale.US, "   %s: revenue %.2f, expenses %.2f, profit %.2f, margin %s%n",
                m.month(), m.revenue(), m.expenses(), m.netProfit(), fmt(m.margin())));
    }

    /** null means the figure is unavailable. */
    private static String fmt(Double value) {
        return value == null ? "unavailable" : String.format(Locale.US, "%.2f", value);
    }
}