package com.payflux.vnpay_adapter.application;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TxnDateCache {
    private final ConcurrentMap<String,String> store = new ConcurrentHashMap<>();

    public void put(String txnRef, String createDate) {
        store.put(txnRef, createDate);
    }

    public String get(String txnRef) {
        return store.get(txnRef);
    }
}
