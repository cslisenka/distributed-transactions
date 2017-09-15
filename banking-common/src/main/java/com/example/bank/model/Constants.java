package com.example.bank.model;

public interface Constants {

    String QUEUE_TRANSFER = "MONEY.TRANSFER.QUEUE";
    String QUEUE_XA_TRANSFER = "MONEY.TRANSFER.XA.QUEUE";

    String QUEUE_CACHE_UPDATE = "CACHE.UPDATE.QUEUE";
    String QUEUE_XA_CACHE_UPDATE = "CACHE.UPDATE.XA.QUEUE";

    String HAZELCAST_ACCOUNTS = "account";
    String HAZELCAST_TRANSFERS = "transfer";
    String HAZELCAST_PENDING_TRANSFERS = "pendingTransfer";
}