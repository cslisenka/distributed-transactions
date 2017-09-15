package com.example.bank.api;

import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactory;
import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactoryImpl;
import ch.maxant.generic_jca_adapter.TransactionAssistant;
import com.example.bank.model.AccountDTO;
import com.example.bank.model.MoneyTransferDTO;
import com.example.bank.model.OverdraftException;
import com.example.bank.service.HTTPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.MapMessage;
import javax.jms.Queue;
import java.util.Date;

public abstract class AbstractBackendAPI {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBackendAPI.class);

    @Autowired
    protected HTTPService ws;

    protected void doTransfer(MoneyTransferDTO request) throws Exception {
        AccountDTO local = withdrawLocalDB(request);
        sendCacheUpdate(local, request);

        String bank = request.getToBank();
        if ("nonxa".equals(bank)) {
            AccountDTO accountTo = depositLocalDB(request);
            sendCacheUpdate(accountTo, request);
        } else if ("partner".equals(bank)) {
            depositPartnerDB(request);
        } else if ("external".equals(bank)) {
            if (isXA()) {
                depositExternalWSXA(request);
            } else {
                depositExternalWSNonXA(request);
            }
        }
    }

    private AccountDTO withdrawLocalDB(MoneyTransferDTO request) throws OverdraftException {
        // Lock account in local DB
        AccountDTO account = local().queryForObject("select * from account where identifier = ? for update",
                new AccountDTO.AccountRowMapper(), request.getFrom());

        // Only check for overdraft if we transfer money from nonxa account
        if (account.getBalance() < request.getAmount()) {
            throw new OverdraftException(request.getFrom());
        }

        // Change balance nonxa account + save transfer in nonxa DB
        local().update("UPDATE account SET balance = ? WHERE identifier = ?",
                account.getBalance() - request.getAmount(), request.getFrom());

        Date time = new Date();
        local().update("insert into money_transfer (transfer_id, account_from, account_to, bank_to, amount, date_time) " +
                "values (?, ?, ?, ?, ?, ?)", request.getTransferId(), request.getFrom(), request.getTo(), "partner", request.getAmount(), time);

        // Sending cache update event
        account.setBalance(account.getBalance() - request.getAmount());
        request.setDateTime(time);
        return account;
    }

    private AccountDTO depositLocalDB(MoneyTransferDTO request) {
        AccountDTO accountTo = local().queryForObject("select * from account where identifier = ? for update",
                new AccountDTO.AccountRowMapper(), request.getTo());

        local().update("update account set balance = ? where identifier = ?",
                accountTo.getBalance() + request.getAmount(), request.getFrom());

        accountTo.setBalance(accountTo.getBalance() + request.getAmount());
        return accountTo;
    }

    private void depositPartnerDB(MoneyTransferDTO request) throws OverdraftException {
        // Query for locking
        AccountDTO partnerAccount = partner().queryForObject("select * from partner_account where identifier = ? for update",
                new AccountDTO.AccountRowMapper(), request.getTo());

        // Change balance partner account
        partner().update("UPDATE partner_account SET balance = ? WHERE identifier = ?",
                partnerAccount.getBalance() + request.getAmount(), request.getTo());

        // save transfer in partner DB
        partner().update("INSERT INTO partner_money_transfer (transfer_id, account, external_account, amount, status, date_time) " +
                "VALUES (?, ?, ?, ?, ?, ?)", request.getTransferId(), request.getTo(), request.getFrom(), request.getAmount(), "CONFIRMED", new Date());
    }

    protected void depositExternalWSXA(MoneyTransferDTO request) throws Exception {
        // For demonstration purposes we can shut down partner web-services and show that transaction was rolled back (if our SQL is first)
        // If our database (next operation) rejects payment - the cancel should be called on web-service
        // TransactionManager will hold XA transaction on MySQL and JMS side and repeat retries to WS-confirm until timeout is passed, then call WS-cancel
        BasicTransactionAssistanceFactory transferServiceTransactionFactory = new BasicTransactionAssistanceFactoryImpl("xa/transferService"); // TODO how to avoid using JNDI?
        try (TransactionAssistant transactionAssistant = transferServiceTransactionFactory.getTransactionAssistant()){
            String partnerTransferId = transactionAssistant.executeInActiveTransaction(xaTransactionId -> {
                // In case of failure - TransactionManager must call WS-reject
                return ws.reserveMoney(request.getFrom(), request.getTo(), request.getAmount(), xaTransactionId);
            });

            // TODO save it in our database may be
            logger.info("Partner transfer id = {}", partnerTransferId);
        }
    }

    protected void depositExternalWSNonXA(MoneyTransferDTO request) throws Exception {
        // NO guarantee if at least one of the WS-calls fails
        ws.reserveMoney(request.getFrom(), request.getTo(), request.getAmount(), request.getTransferId());
        ws.confirm(request.getTransferId());
    }

    private void sendCacheUpdate(AccountDTO account, MoneyTransferDTO transfer) {
        jms().send(cacheUpdateQueue(), session -> {
            MapMessage msg = session.createMapMessage();
            account.to(msg);
            transfer.to(msg);
            return msg;
        });
    }

    protected abstract JdbcTemplate local();
    protected abstract JdbcTemplate partner();
    protected abstract JmsTemplate jms();
    protected abstract Queue cacheUpdateQueue();
    protected abstract boolean isXA();
}