package com.mcs.hotel.repository;

import com.mcs.hotel.model.Bill;

import java.util.List;
import java.util.Optional;

public interface BillRepository {
    Bill save(Bill bill);
    List<Bill> findAll();
    Optional<Bill> findById(String id);
    void deleteById(String id);
}