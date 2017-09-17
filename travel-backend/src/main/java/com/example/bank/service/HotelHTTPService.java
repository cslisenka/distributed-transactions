package com.example.bank.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for transferring money between our bank and external organizations
 * who provide WEB-Service API
 */
@Service
public class HotelHTTPService {

    private static final Logger logger = LoggerFactory.getLogger(HotelHTTPService.class);

    public static final String RESERVE = "http://localhost:8090/transfer/reserve";
    public static final String CONFIRM = "http://localhost:8090/transfer/{requestId}/confirm";
    public static final String CANCEL = "http://localhost:8090/transfer/{requestId}/cancel";
    public static final String UNFINISHED = "http://localhost:8090/transfer/unfinished";

    // TODO store in DB to make durable - MUST MUST MUST - after restart we are not able to restore any distributed TX
    // TODO when could we clean it?
    // Stores mapping of XA transaction IDs to the transferIDs returned by service
    private ConcurrentHashMap<String, String> xaToTransferIds = new ConcurrentHashMap<>();
    private RestTemplate restTemplate = new RestTemplate();

    public String reserveMoney(String from, String to, int amount, String xaId) {
        try {
            String response = restTemplate.postForObject(RESERVE, new ReserveMoneyRequest(from, to, amount), String.class);
            xaToTransferIds.put(xaId, response);
            logger.info("SUCCESS {} [from='{}' to='{}' amount='{}'] response='{}', xaId={}", RESERVE, from, to, amount, response, xaId);
            return response;
        } catch (Exception e) {
            logger.error("ERROR {} [from='{}' to='{}' amount='{}'] exception='{}', xaId={}", RESERVE, from, to, amount, e.getMessage(), xaId);
            throw e;
        }
    }

    // TransactionManager will keep calling confirm until the transaction timeout is reached
    public void confirm(String xaId) {
        String transferId = xaToTransferIds.get(xaId);

        try {
            // TODO add monitoring for retrues (collect calls to HashMap, or lis - display to users)
            restTemplate.postForLocation(CONFIRM, null, transferId);
            logger.info("SUCCESS {} [xaId='{}', transferId='{}']", xaId, CONFIRM, transferId);
        } catch (Exception e) {
            logger.error("ERROR {} [xaId='{}', transferId='{}' exception='{}']", xaId, CONFIRM, transferId, e.getMessage());
            throw e;
        }
    }

    public void cancel(String xaId) {
        String transferId = xaToTransferIds.get(xaId);

        if (transferId != null) {
            try {
                restTemplate.postForLocation(CANCEL, null, transferId);
                logger.info("SUCCESS {} xaId='{}', transferId='{}'", xaId, CANCEL, transferId);
            } catch (Exception e) {
                logger.error("ERROR {} xaId='{}', transferId='{}' exception='{}'", xaId, CANCEL, transferId, e.getMessage());
                throw e;
            }
        } else {
            logger.warn("SKIP {} as no transfer_id received before,  xaId={}", CANCEL, xaId);
        }
    }

    // For using by XA recovery mechanism
    public List<String> getUnfinishedTransfers() {
        // TODO get xaIDs by transferIds
        try {
            List<String> response = restTemplate.getForObject(UNFINISHED, ArrayList.class);
            logger.info("SUCCESS {} response='{}'", UNFINISHED, response);
            return response;
        } catch (Exception e) {
            logger.error("ERROR {} exception='{}'", UNFINISHED, e.getMessage());
            throw e;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReserveMoneyRequest {

        private String from;
        private String to;
        private int amount;

        public ReserveMoneyRequest() {
        }

        public ReserveMoneyRequest(String from, String to, int amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }
}