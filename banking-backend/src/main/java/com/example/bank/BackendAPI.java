package com.example.bank;

import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactory;
import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactoryImpl;
import ch.maxant.generic_jca_adapter.TransactionAssistant;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.example.bank.model.AccountDTO;
import com.example.bank.model.MoneyTransferDTO;
import com.example.bank.model.OverdraftException;
import com.example.bank.service.HTTPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import java.util.Date;
import java.util.UUID;

@RestController
public class BackendAPI implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(BackendAPI.class);

    @Autowired
    private HTTPService externalHttpService;

    @Autowired
    @Qualifier("local")
    private JdbcTemplate local;

    @Autowired
    @Qualifier("partner")
    private JdbcTemplate partner;

    @Autowired
    @Qualifier("xaJmsTemplate")
    private JmsTemplate jms;

    @Autowired
    @Qualifier("cacheUpdateQueue")
    private Queue cacheUpdateQueue;

    @Autowired
    private UserTransactionManager tm;

    @PostMapping("/transfer")
    public String transferMoney(@RequestBody MoneyTransferDTO request) throws Exception {
        tm.begin(); // Start JTA transaction
        logger.info("We are already inside JTA transaction {}", tm.getTransaction());
        try {
            String transferID = UUID.randomUUID().toString();
            request.setTransferId(transferID);
            doTransfer(request);
            tm.commit();
            return transferID;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            tm.rollback();
            throw e;
        }
    }

    // We are already in JTA transaction because we are using Atomikos JMS container
    @Override
    public void onMessage(Message message) {
        try {
            logger.info("We are already inside JTA transaction {}", tm.getTransaction());
            MapMessage map = (MapMessage) message;
            doTransfer(MoneyTransferDTO.from(map));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void doTransfer(MoneyTransferDTO request) throws Exception {
        AccountDTO local = withdrawLocalDB(request);
        sendCacheUpdate(local, request);

        String bank = request.getToBank();
        if ("local".equals(bank)) {
            AccountDTO accountTo = depositLocalDB(request);
            sendCacheUpdate(accountTo, request);
        } else if ("partner".equals(bank)) {
            depositPartnerDB(request);
        } else if ("external".equals(bank)) {
            depositExternalWS(request);
        }
    }

    public AccountDTO withdrawLocalDB(MoneyTransferDTO request) throws OverdraftException {
        // Lock local account
        AccountDTO account = local.queryForObject("select * from account where identifier = ? for update",
                new AccountDTO.AccountRowMapper(), request.getFrom());

        // Only check for overdraft if we transfer money from local account
        if (account.getBalance() < request.getAmount()) {
            throw new OverdraftException(request.getFrom());
        }

        // Change balance local account + save transfer in local DB
        local.update("UPDATE account SET balance = ? WHERE identifier = ?",
                account.getBalance() - request.getAmount(), request.getFrom());

        Date time = new Date();
        local.update("insert into money_transfer (transfer_id, account_from, account_to, bank_to, amount, date_time) " +
                "values (?, ?, ?, ?, ?, ?)", request.getTransferId(), request.getFrom(), request.getTo(), "partner", request.getAmount(), time);

        // Sending cache update event
        account.setBalance(account.getBalance() - request.getAmount());
        request.setDateTime(time);
        return account;
    }

    private AccountDTO depositLocalDB(MoneyTransferDTO request) {
        AccountDTO accountTo = local.queryForObject("select * from account where identifier = ? for update",
                new AccountDTO.AccountRowMapper(), request.getTo());

        local.update("update account set balance = ? where identifier = ?",
                accountTo.getBalance() + request.getAmount(), request.getFrom());

        accountTo.setBalance(accountTo.getBalance() + request.getAmount());
        return accountTo;
    }

    public void depositPartnerDB(MoneyTransferDTO request) throws OverdraftException {
        // Query for locking
        AccountDTO partnerAccount = partner.queryForObject("select * from partner_account where identifier = ? for update",
                new AccountDTO.AccountRowMapper(), request.getTo());

        // Change balance partner account
        partner.update("UPDATE partner_account SET balance = ? WHERE identifier = ?",
                partnerAccount.getBalance() + request.getAmount(), request.getTo());

        // save transfer in partner DB
        partner.update("INSERT INTO partner_money_transfer (transfer_id, account, external_account, amount, status, date_time) " +
                "VALUES (?, ?, ?, ?, ?, ?)", request.getTransferId(), request.getTo(), request.getFrom(), request.getAmount(), "CONFIRMED", new Date());
    }

    public void depositExternalWS(MoneyTransferDTO request) throws Exception {
        // For demonstration purposes we can shut down partner web-services and show that transaction was rolled back (if our SQL is first)
        // If our database (next operation) rejects payment - the cancel should be called on web-service
        // TransactionManager will hold XA transaction on MySQL and JMS side and repeat retries to WS-confirm until timeout is passed, then call WS-cancel
        BasicTransactionAssistanceFactory transferServiceTransactionFactory = new BasicTransactionAssistanceFactoryImpl("xa/transferService"); // TODO how to avoid using JNDI?
        try (TransactionAssistant transactionAssistant = transferServiceTransactionFactory.getTransactionAssistant()){
            String partnerTransferId = transactionAssistant.executeInActiveTransaction(xaTransactionId -> {
                // In case of failure - TransactionManager must call WS-reject
                return externalHttpService.reserveMoney(request.getFrom(), request.getTo(), request.getAmount(), xaTransactionId);
            });

            // TODO use log + save it in our database may be
            logger.info("Partner transfer id = {}", partnerTransferId);
        }
    }

    protected void sendCacheUpdate(AccountDTO account, MoneyTransferDTO transfer) {
        jms.send(cacheUpdateQueue, session -> {
            MapMessage msg = session.createMapMessage();
            account.to(msg);
            transfer.to(msg);
            return msg;
        });
    }
}