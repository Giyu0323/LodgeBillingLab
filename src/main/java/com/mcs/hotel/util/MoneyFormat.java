package com.mcs.hotel.util;

import java.util.Locale;

/** Formats amounts the same way everywhere, e.g. "K 6,212.50". */
public final class MoneyFormat {

    private MoneyFormat() {} // utility class: no instances

    /** Locale.US forces a comma for thousands and a dot for decimals on every PC. */
    public static String k(double amount) {
        return String.format(Locale.US, "K %,.2f", amount);
    }
}