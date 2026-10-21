package com.mcs.hotel.controller;

/** Implemented by any screen whose list the top-bar search box can filter. */
public interface Searchable {
    void setFilter(String text);
}