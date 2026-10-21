package com.mcs.hotel.repository;

import com.mcs.hotel.model.Bill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryBillRepository implements BillRepository {

    private final Map<String, Bill> bills = new LinkedHashMap<>();

    @Override
    public Bill save(Bill bill) {
        bills.put(bill.id(), bill);
        return bill;
    }

    @Override
    public List<Bill> findAll() {
        return List.copyOf(bills.values());
    }

    @Override
    public Optional<Bill> findById(String id) {
        return Optional.ofNullable(bills.get(id));
    }

    @Override
    public void deleteById(String id) {
        bills.remove(id);
    }
}