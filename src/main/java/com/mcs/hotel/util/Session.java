package com.mcs.hotel.util;

import com.mcs.hotel.model.User;

public final class Session {

    private static User currentUser;

    private Session() {}

    public static void setUser(User user) { currentUser = user; }
    public static User getUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }
    public static void clear() { currentUser = null; }
}