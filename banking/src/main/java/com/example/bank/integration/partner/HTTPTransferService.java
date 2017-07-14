package com.example.bank.integration.partner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for transferring money between our bank and external organizations
 * who provide WEB-Service API
 */
@Service
public class HTTPTransferService {

    public static final String RESERVE = "http://localhost:8090/transfer/reserve";
    public static final String CONFIRM = "http://localhost:8090/transfer/{requestId}/confirm";
    public static final String CANCEL = "http://localhost:8090/transfer/{requestId}/cancel";
    public static final String UNFINISHED = "http://localhost:8090/transfer/unfinished";

    @Autowired
    private RestTemplate restTemplate;

    public String reserveMoney(String from, String to, int amount) {
        return restTemplate.postForObject(RESERVE, new ReserveMoneyRequest(from, to, amount), String.class);
    }

    public void confirm(String transferId) {
        restTemplate.postForLocation(CONFIRM, null, transferId);
    }

    public void cancel(String transferId) {
        restTemplate.postForLocation(CANCEL, null, transferId);
    }

    public List<String> getUnfinishedTransfers() {
        return restTemplate.getForObject(UNFINISHED, ArrayList.class);
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