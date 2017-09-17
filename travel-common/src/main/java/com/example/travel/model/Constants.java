package com.example.travel.model;

public interface Constants {

    String QUEUE_TRANSFER = "MONEY.TRANSFER.QUEUE";
    String QUEUE_XA_TRANSFER = "MONEY.TRANSFER.XA.QUEUE";

    String QUEUE_XA_BOOKING_REQUEST = "BOOKING.REQUEST.XA";

    String QUEUE_CACHE_UPDATE = "CACHE.UPDATE.QUEUE";
    String QUEUE_XA_CACHE_UPDATE = "CACHE.UPDATE.XA.QUEUE";

    String QUEUE_XA_BOOKING_RESPONSE = "BOOKING.RESPONSE.XA";

    String HAZELCAST_ACCOUNTS = "account";
    String HAZELCAST_TRANSFERS = "transfer";
    String HAZELCAST_PENDING_TRANSFERS = "pendingTransfer";

    String BOOKING_FINISHED = "booking";
    String BOOKING_IN_PROGRESS = "bookingInProgress";
    String BOOKING_ITEM = "bookingItem";
}