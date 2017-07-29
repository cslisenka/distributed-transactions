package com.example.bank.model;

public interface Constants {

    String QUEUE_TRANSFER = "MONEY.TRANSFER.QUEUE";
    String QUEUE_CACHE_UPDATE = "CACHE.UPDATE.QUEUE";

    String HAZELCAST_ACCOUNTS = "account";
    String HAZELCAST_TRANSFERS = "transfer";
    String HAZELCAST_PENDING_TRANSFERS = "pendingTransfer";
}