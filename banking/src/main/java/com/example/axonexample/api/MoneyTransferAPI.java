package com.example.axonexample.api;

import com.example.axonexample.model.*;
import com.example.axonexample.service.LocalTransferService;
import com.example.axonexample.service.PartnerTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import javax.jms.MapMessage;
import javax.jms.Queue;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class MoneyTransferAPI {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MoneyTransferRepository moneyTransferRepository;

    @Autowired
    private PartnerMoneyTransferRepository partnerMoneyTransferRepository;

    @Autowired
    private LocalTransferService localTransferService;

    @Autowired
    private PartnerTransferService partnerTransferService;

    @Autowired
    private Queue requestQueue;

    @Autowired
    private JmsTemplate jmsTemplate;

    @GetMapping
    public List<Account> getAccounts() {
        return accountRepository.findAll();
    }

    @GetMapping("/transfers")
    public List<MoneyTransfer> getMoneyTransfers() { //@PathVariable String accountIdentifier
        return moneyTransferRepository.findAll();
    }

    @GetMapping("/partnerTransfers")
    public List<PartnerMoneyTransfer> getPartnerMoneyTransfers() {
        return partnerMoneyTransferRepository.findAll();
    }

    @PostMapping("/transferMoneyLocal")
    public MoneyTransfer transferMoney(@RequestBody Map<String, String> request) throws OverdraftException {
        return localTransferService.doTransfer(request.get("from"), request.get("to"), Integer.parseInt(request.get("amount")));
    }

    @PostMapping("/transferMoneyPartner")
    public PartnerMoneyTransfer transferMoneyPartner(@RequestBody Map<String, String> request) throws OverdraftException {
        return partnerTransferService.doTransfer(request.get("from"), request.get("to"), Integer.parseInt(request.get("amount")));
    }

    @PostMapping("/queuedTransferMoney")
    public String queueMoneyTransfer(@RequestBody Map<String, String> request) {
        // TODO store pending transfer in REDIS - in transaction with JMS message
        // This may be needed for high performance as well if we want to temporary not accept new payments for DB maintenance
        String transferId = UUID.randomUUID().toString(); // UUID to make request idempotent
        // TODO save UUID in DB

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