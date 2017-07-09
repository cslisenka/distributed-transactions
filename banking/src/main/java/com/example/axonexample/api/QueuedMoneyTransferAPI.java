package com.example.axonexample.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.MapMessage;
import javax.jms.Queue;
import java.util.Map;
import java.util.UUID;

@RestController
public class QueuedMoneyTransferAPI {

    @Autowired
    private Queue requestQueue;

    @Autowired
    private JmsTemplate jmsTemplate;

    @PostMapping("/queuedTransferMoney")
    public String queueMoneyTransfer(@RequestBody Map<String, String> request) {
        String transferId = UUID.randomUUID().toString();

        jmsTemplate.send(requestQueue, session -> {
            MapMessage message = session.createMapMessage();
            message.setString("transferId", transferId);
            message.setString("from", request.get("from"));
            message.setString("to", request.get("to"));
            message.setInt("amount", Integer.parseInt(request.get("amount")));
            return message;
        });

        // TODO return status: success, error
        return transferId; // We can add payment ID to cache for displaying to user, or just return it to frontend
    }
}