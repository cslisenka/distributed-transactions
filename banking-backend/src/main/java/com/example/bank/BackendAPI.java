package com.example.bank;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.example.bank.model.MoneyTransferDTO;
import com.example.bank.service.AbstractTransferService;
import com.example.bank.service.ExternalService;
import com.example.bank.service.LocalService;
import com.example.bank.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.UUID;

@RestController
public class BackendAPI implements MessageListener {

    @Autowired
    private LocalService local;

    @Autowired
    private PartnerService partner;

    @Autowired
    private ExternalService external;

    @Autowired
    private UserTransactionManager tm;

    @PostMapping("/transfer")
    public String transferMoney(@RequestBody MoneyTransferDTO request) throws Exception {
        tm.begin(); // Start JTA transaction
        try {
            String transferID = UUID.randomUUID().toString();
            request.setTransferId(transferID);
            doTransfer(request);
            tm.commit();
            return transferID;
        } catch (Exception e) {
            tm.rollback();
            throw e;
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            // We are already in JTA transaction because we are using Atomikos JMS container
            // TODO change to log
            System.out.println("We are already inside JTA transaction " + tm.getTransaction());

            MapMessage map = (MapMessage) message;
            doTransfer(MoneyTransferDTO.createFrom(map));
        } catch (Exception e) {
            // TODO log as error
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void doTransfer(MoneyTransferDTO request) throws Exception {
        try {
            getService(request.getToBank())
                .doTransfer(request);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO send cache reload event (may be)
            throw e;
        }
    }

    public AbstractTransferService getService(String bank) {
        switch (bank) {
            case "local":
                return local;
            case "partner":
                return partner;
            case "external":
                return external;
            default:
                throw new RuntimeException("incorrect bank " + bank);
        }
    }
}