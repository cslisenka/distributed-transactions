package com.example.bank.model;

public interface Constants {

    String TRANSFER_QUEUE = "MONEY.TRANSFER.QUEUE";
    String CACHE_UPDATE_QUEUE = "CACHE.UPDATE.QUEUE";

    String HAZELCAST_ACCOUNTS = "account";
    String HAZELCAST_TRANSFERS = "transfer";
    String HAZELCAST_PENDING_TRANSFERS = "pendingTransfer";
}
