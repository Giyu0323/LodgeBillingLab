package com.mcs.hotel.controller;

import com.mcs.hotel.model.AnalyticsReport;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.util.MoneyFormat;
import com.mcs.hotel.view.AnalyticsDialogs;
import com.mcs.hotel.view.AnalyticsPanel;
import com.mcs.hotel.view.AnalyticsPanel.Card;
import com.mcs.hotel.view.AnalyticsPanel.Kpi;
import com.mcs.hotel.view.AnalyticsPanel.Tone;
import com.mcs.hotel.view.Dialogs;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Connects AnalyticsPanel to AnalyticsService.
 * The service works the figures out (and says "unavailable" when it cannot).
 * This class only turns them into text and colours. When the period changes,
 * everything is redrawn from a fresh report.
 */
public class AnalyticsController {

    private static final DateTimeFormatter MONTH_NAME =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private static final DateTimeFormatter CHART_MONTH =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final AnalyticsPanel view;
    private final AppServices services;

    public AnalyticsController(AnalyticsPanel view, AppServices services) {
        this.view = view;
        this.services = services;
        view.onPeriodChanged(period -> refresh());
        view.onSetCapital(this::setCapital);
        refresh();
    }

    /** Works out the report for the chosen period and puts every figure on screen. */
    private void refresh() {
        AnalyticsReport r = services.analytics().report(view.getPeriod());

        view.setRangeText("Showing " + r.start() + " to " + r.end());

        // ---- The four summary cards ----
        view.setCard(Card.REVENUE, money(r.revenue()), true,
                change(r.revenueChange(), "%"), tone(r.revenueChange(), true));
        view.setCard(Card.EXPENSES, money(r.expenses()), true,
                change(r.expensesChange(), "%"), tone(r.expensesChange(), false)); // spending more is not good news
        view.setCard(Card.PROFIT, money(r.netProfit()), true,
                change(r.profitChange(), "%"), tone(r.profitChange(), true));
        view.setCard(Card.MARGIN,
                r.profitMargin() == null ? "Unavailable" : percent(r.profitMargin()),
                r.profitMargin() != null,
                change(r.marginChange(), " pp"), tone(r.marginChange(), true));

        // ---- The four KPIs ----
        if (r.roi() == null) {
            view.setKpi(Kpi.ROI, "Unavailable", false, "Enter the invested capital to work out ROI.");
        } else {
            view.setKpi(Kpi.ROI, percent(r.roi()), true,
                    "Profit ÷ invested capital of " + MoneyFormat.k(services.analytics().getInvestedCapital()));
        }

        if (r.occupancyRate() == null) {
            view.setKpi(Kpi.OCCUPANCY, "Unavailable", false, "No rooms to measure.");
        } else {
            view.setKpi(Kpi.OCCUPANCY, percent(r.occupancyRate()), true,
                    r.roomNightsSold() + " of " + r.roomNightsAvailable() + " room-nights sold");
        }

        if (r.averageDailyRate() == null) {
            view.setKpi(Kpi.ADR, "Unavailable", false, "No room-nights were billed in this period.");
        } else {
            view.setKpi(Kpi.ADR, money(r.averageDailyRate()), true, "Room revenue ÷ room-nights billed");
        }

        if (r.revPar() == null) {
            view.setKpi(Kpi.REVPAR, "Unavailable", false, "No rooms to measure.");
        } else {
            view.setKpi(Kpi.REVPAR, money(r.revPar()), true, "Room revenue ÷ available room-nights");
        }

        // ---- The monthly table ----
        List<AnalyticsPanel.MonthRow> rows = r.months().stream()
                .map(m -> new AnalyticsPanel.MonthRow(
                        m.month().format(MONTH_NAME),
                        money(m.revenue()),
                        money(m.expenses()),
                        money(m.netProfit()),
                        m.margin() == null ? "N/A" : percent(m.margin()),
                        m.netProfit() < 0))
                .toList();
        view.setMonths(rows);

        // ---- The charts: the same monthly figures, as plain numbers ----
        List<String> labels = r.months().stream()
                .map(m -> m.month().format(CHART_MONTH))
                .toList();
        List<Double> revenue = r.months().stream().map(m -> m.revenue()).toList();
        List<Double> expenses = r.months().stream().map(m -> m.expenses()).toList();
        List<Double> profit = r.months().stream().map(m -> m.netProfit()).toList();
        view.setCharts(labels, revenue, expenses, profit, r.roomRevenue(), r.extrasRevenue());
    }

    /** The "Set capital" button: ask for the amount, save it, and redraw (ROI appears). */
    private void setCapital() {
        double current = services.analytics().getInvestedCapital();
        String shown = current > 0 ? String.format(Locale.US, "%.2f", current) : "";

        Optional<String> answer = AnalyticsDialogs.askInvestedCapital(shown);
        if (answer.isEmpty()) {
            return; // the dialog was cancelled
        }
        try {
            services.analytics().setInvestedCapital(parseAmount(answer.get()));
            refresh();
        } catch (IllegalArgumentException ex) {
            Dialogs.error(ex.getMessage());
        }
    }

    // ---------- Turning numbers into text ----------

    /** "K 1,250.00", or "-K 237.50" when the amount is negative. */
    private String money(double amount) {
        return amount < 0 ? "-" + MoneyFormat.k(-amount) : MoneyFormat.k(amount);
    }

    private String percent(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    /** "▲ +12.8%  vs. previous period", or a plain note when there is nothing to compare with. */
    private String change(Double value, String unit) {
        if (value == null) {
            return "No earlier data to compare";
        }
        if (Math.abs(value) < 0.05) {
            return "No change vs. previous period";
        }
        String arrow = value > 0 ? "▲ +" : "▼ ";
        return arrow + String.format(Locale.US, "%.1f", value) + unit + "  vs. previous period";
    }

    /** Green for good news, red for bad. "goodWhenUp" is false for expenses, where more is worse. */
    private Tone tone(Double value, boolean goodWhenUp) {
        if (value == null || Math.abs(value) < 0.05) {
            return Tone.NEUTRAL;
        }
        boolean up = value > 0;
        return up == goodWhenUp ? Tone.GOOD : Tone.BAD;
    }

    /** Turns typed text into a number. A blank box counts as 0, which clears the capital. */
    private double parseAmount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            double value = Double.parseDouble(text.trim().replace(",", ""));
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("The amount must be a number.");
        }
    }
}