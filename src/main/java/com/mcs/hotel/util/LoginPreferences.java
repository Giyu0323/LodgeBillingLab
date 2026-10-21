package com.mcs.hotel.util;

import java.util.prefs.Preferences;

/**
 * Remembers the last username across launches, for the "Remember me" box.
 * Only the USERNAME is saved, never the password. Java keeps it in a small
 * per-user setting on this computer (the registry on Windows).
 * Everything else in the app stays in memory.
 */
public final class LoginPreferences {

    private static final Preferences PREFS = Preferences.userNodeForPackage(LoginPreferences.class);
    private static final String KEY = "rememberedUsername";

    private LoginPreferences() {} // utility class: no instances

    /** The remembered username, or an empty string if there is none. */
    public static String getSavedUsername() {
        try {
            return PREFS.get(KEY, "");
        } catch (RuntimeException ex) {
            return ""; // a settings problem must never stop someone from logging in
        }
    }

    public static void saveUsername(String username) {
        try {
            PREFS.put(KEY, username);
        } catch (RuntimeException ex) {
            // ignore: Remember me is a convenience, not a requirement
        }
    }

    /** Forgets the username (the box was unticked). */
    public static void clear() {
        try {
            PREFS.remove(KEY);
        } catch (RuntimeException ex) {
            // ignore, as above
        }
    }
}