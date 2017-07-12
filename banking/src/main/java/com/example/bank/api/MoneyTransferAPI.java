package com.example.bank.api;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.example.bank.model.*;
import com.example.bank.model.mapper.AccountRowMapper;
import com.example.bank.model.mapper.MoneyTransferRowMapper;
import com.example.bank.model.mapper.PartnerMoneyTransferRowMapper;
import com.example.bank.integration.partner.sql.PartnerTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import javax.jms.MapMessage;
import javax.jms.Queue;
import javax.transaction.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class MoneyTransferAPI {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private LocalTransferService localTransferService;

    @Autowired
    @Qualifier("partnerTransferService")
    private PartnerTransferService partnerTransferService;

    @Autowired
    @Qualifier("xaPartnerTransferService")
    private PartnerTransferService xaPartnerTransferService;

    @Autowired
    @Qualifier("localJdbc")
    private JdbcTemplate jdbc;

    @Autowired
    private Queue requestQueue;

    @Autowired
    private JmsTemplate jmsTemplate;

    @GetMapping
    public List<Account> getAccounts() {
        return jdbc.query("select * from account", new AccountRowMapper());
    }

    @GetMapping("/transfers")
    public List<MoneyTransfer> getMoneyTransfers() { //@PathVariable String accountIdentifier
        return jdbc.query("select * from money_transfer", new MoneyTransferRowMapper());
    }

    @GetMapping("/partnerTransfers")
    public List<PartnerMoneyTransfer> getPartnerMoneyTransfers() {
        return jdbc.query("select * from partner_money_transfer", new PartnerMoneyTransferRowMapper());
    }

    @PostMapping("/transferMoneyLocal")
    public void transferMoney(@RequestBody Map<String, String> request) throws OverdraftException {
        String transferId = UUID.randomUUID().toString();
        localTransferService.doTransfer(transferId, request.get("from"),
                request.get("to"), Integer.parseInt(request.get("amount")));
    }

    @PostMapping("/transferMoneyToPartner")
    public void transferMoneyToPartner(@RequestBody Map<String, String> request) throws OverdraftException {
        String transferId = UUID.randomUUID().toString();
        partnerTransferService.doTransfer(transferId, request.get("from"),
                request.get("to"), Integer.parseInt(request.get("amount")));
    }

    @PostMapping("/xaTransferMoneyToPartner")
    public void xaTransferMoneyToPartner(@RequestBody Map<String, String> request) throws OverdraftException, SystemException, HeuristicRollbackException, HeuristicMixedException, RollbackException, NotSupportedException {
        UserTransaction tx = context.getBean(UserTransactionImp.class); // JTA transaction
        tx.begin();
        try {
            String transferId = UUID.randomUUID().toString();
            xaPartnerTransferService.doTransfer(transferId, request.get("from"),
                    request.get("to"), Integer.parseInt(request.get("amount")));
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @PostMapping("/queuedTransferMoney")
    public String queueMoneyTransfer(@RequestBody Map<String, String> request) {
        // TODO store pending transfer in REDIS - in transaction with JMS message
        // This may be needed for high performance as well if we want to temporary not accept new payments for DB maintenance
        String transferId = UUID.randomUUID().toString(); // UUID to make request idempotent

        jmsTemplate.send(requestQueue, session -> {
            MapMessage message = session.createMapMessage();
            message.setString("transfer_id", transferId);
            message.setString("from", request.get("from"));
            message.setString("to", request.get("to"));
            message.setInt("amount", Integer.parseInt(request.get("amount")));
            return message;
        });

        // TODO return status: success, error
        return transferId; // We can add payment ID to cache for displaying to user, or just return it to frontend
    }
}