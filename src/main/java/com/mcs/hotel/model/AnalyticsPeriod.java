package com.mcs.hotel.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * The reporting periods in the Analytics dropdown, and the dates each one covers.
 * "months" is how many months the period spans; it is also how far back to look
 * when comparing with the period before (0 means All Time, which has no comparison).
 */
public enum AnalyticsPeriod {

    THIS_MONTH("This Month", 1),
    LAST_MONTH("Last Month", 1),
    LAST_3_MONTHS("Last 3 Months", 3),
    LAST_6_MONTHS("Last 6 Months", 6),
    THIS_YEAR("This Year", 12),
    ALL_TIME("All Time", 0);

    /** A span of days. Both ends are included. */
    public record Range(LocalDate start, LocalDate end) {

        public long days() {
            return ChronoUnit.DAYS.between(start, end) + 1;
        }
    }

    private final String label;
    private final int months;

    AnalyticsPeriod(String label, int months) {
        this.label = label;
        this.months = months;
    }

    /**
     * The dates this period covers.
     *
     * @param today    today's date
     * @param earliest the oldest date in the records (used by All Time), or null if there are none
     */
    public Range range(LocalDate today, LocalDate earliest) {
        YearMonth thisMonth = YearMonth.from(today);
        return switch (this) {
            case THIS_MONTH -> new Range(thisMonth.atDay(1), today);
            case LAST_MONTH -> {
                YearMonth last = thisMonth.minusMonths(1);
                yield new Range(last.atDay(1), last.atEndOfMonth());
            }
            case LAST_3_MONTHS -> new Range(thisMonth.minusMonths(2).atDay(1), today);
            case LAST_6_MONTHS -> new Range(thisMonth.minusMonths(5).atDay(1), today);
            case THIS_YEAR -> new Range(LocalDate.of(today.getYear(), 1, 1), today);
            case ALL_TIME -> {
                LocalDate from = (earliest == null || earliest.isAfter(today)) ? today : earliest;
                yield new Range(from.withDayOfMonth(1), today);
            }
        };
    }

    /**
     * The same dates, shifted back by this period's length, for "compared with before".
     * Empty for All Time. A period that ends on the last day of a month is compared
     * with the whole earlier month, so months of different lengths still line up.
     */
    public Optional<Range> previousRange(Range current) {
        if (months == 0) {
            return Optional.empty();
        }
        YearMonth endMonth = YearMonth.from(current.end());
        boolean endsAtMonthEnd = current.end().equals(endMonth.atEndOfMonth());

        LocalDate start = current.start().minusMonths(months);
        LocalDate end = endsAtMonthEnd
                ? endMonth.minusMonths(months).atEndOfMonth()
                : current.end().minusMonths(months);
        return Optional.of(new Range(start, end));
    }

    @Override
    public String toString() {
        return label;
    }
}