package com.mcs.hotel.util;

import com.mcs.hotel.model.Bill;

import java.util.List;
import java.util.Locale;

/** Search rule shared by every bill list: match on bill number, guest name or room number. */
public final class BillFilter {

    private BillFilter() {} // utility class: no instances

    /** Returns the bills whose id, guest name or room contains the text (case-insensitive). */
    public static List<Bill> apply(List<Bill> bills, String text) {
        String needle = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return bills; // no search text: show everything
        }
        return bills.stream()
                .filter(b -> b.id().toLowerCase(Locale.ROOT).contains(needle)
                        || b.guestName().toLowerCase(Locale.ROOT).contains(needle)
                        || b.roomNumber().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }
}