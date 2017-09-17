package com.example.travel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HTTPClient {

    private static final Logger logger = LoggerFactory.getLogger(HTTPClient.class);

    private final String host;
    public final String RESERVE;
    public final String CONFIRM;
    public final String CANCEL;
    public final String UNFINISHED;

    // TODO store in DB to make durable - MUST MUST MUST - after restart we are not able to restore any distributed TX
    // Stores mapping of XA transaction IDs to the booking IDs returned by services
    private ConcurrentHashMap<String, String> xaToBookingIds = new ConcurrentHashMap<>();
    private RestTemplate restTemplate = new RestTemplate();

    public HTTPClient(String host) {
        this.host = host;
        RESERVE = String.format("http://%s/reserve", host);
        CONFIRM = String.format("http://%s/{requestId}/confirm", host);
        CANCEL = String.format("http://%s/{requestId}/cancel", host);
        UNFINISHED = String.format("http://%s/unfinished", host);
    }

    public String reserve(Serializable request, String xaId) {
        try {
            String response = restTemplate.postForObject(RESERVE, request, String.class);
            xaToBookingIds.put(xaId, response);
            logger.info("SUCCESS {} request='{}' response='{}', xaId={}", RESERVE, request, response, xaId);
            return response;
        } catch (Exception e) {
            logger.error("ERROR {} request='{}' exception='{}', xaId={}", RESERVE, request, e.getMessage(), xaId);
            throw e;
        }
    }

    // TransactionManager will keep calling confirm until the transaction timeout is reached
    public void confirm(String xaId) {
        String bookingId = xaToBookingIds.get(xaId);

        try {
            // TODO add monitoring for retries (collect calls to HashMap, or lis - display to users)
            restTemplate.postForLocation(CONFIRM, null, bookingId);
            logger.info("SUCCESS {} [xaId='{}', bookingId='{}']", CONFIRM.replace("{requestId}", bookingId), xaId, bookingId);
        } catch (Exception e) {
            logger.error("ERROR {} [xaId='{}', bookingId='{}' exception='{}']", CONFIRM.replace("{requestId}", bookingId), xaId, bookingId, e.getMessage());
            throw e;
        }
    }

    public void cancel(String xaId) {
        String bookingId = xaToBookingIds.get(xaId);

        if (bookingId != null) {
            try {
                restTemplate.postForLocation(CANCEL, null, bookingId);
                logger.info("SUCCESS {} xaId='{}', bookingId='{}'", CANCEL, xaId,  bookingId);
            } catch (Exception e) {
                logger.error("ERROR {} xaId='{}', bookingId='{}' exception='{}'", CANCEL.replace("{requestId}", bookingId), xaId, bookingId, e.getMessage());
                throw e;
            }
        } else {
            logger.warn("SKIP {} as no booking_id received, xaId={}", CANCEL, xaId);
        }
    }

    // For using by XA recovery mechanism
    public List<String> getTransactionsInProgress() {
        try {
            List<String> unfinishedTransactions = restTemplate.getForObject(UNFINISHED, ArrayList.class);
            logger.info("SUCCESS {} unfinishedTransactions='{}'", UNFINISHED, unfinishedTransactions);
            return unfinishedTransactions;
        } catch (Exception e) {
            logger.error("ERROR {} exception='{}'", UNFINISHED, e.getMessage());
            throw e;
        }
    }
}