package com.example.bank.service;

import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactory;
import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactoryImpl;
import ch.maxant.generic_jca_adapter.TransactionAssistant;
import com.example.bank.model.AccountDTO;
import com.example.bank.model.CacheUpdateDTO;
import com.example.bank.model.MoneyTransferDTO;
import com.example.bank.model.OverdraftException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.jms.Queue;
import java.util.Date;

@Service
public class ExternalService extends AbstractTransferService {

    @Autowired
    @Qualifier("local")
    private JdbcTemplate local;

    @Autowired
    private HTTPService externalHttpService;

    @Autowired
    @Qualifier("xaJmsTemplate")
    private JmsTemplate jms;

    @Autowired
    @Qualifier("cacheUpdateQueue")
    private Queue cacheUpdateQueue;

    @Override
    public void doTransfer(MoneyTransferDTO request) throws Exception {
        doLocal(request);

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
            System.out.println("Partner transfer id = " + partnerTransferId);
        }
    }

    // TODO move to local transfer service
    public void doLocal(MoneyTransferDTO request) throws OverdraftException {
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
        local.update("insert into money_transfers (transfer_id, account_from, account_to, bank_to, amount, date_time) " +
                "values (?, ?, ?, ?, ?, ?)", request.getTransferId(), request.getFrom(), request.getTo(), "partner", request.getAmount(), time);

        // Sending cache update event
        account.setBalance(account.getBalance() - request.getAmount());
        request.setDateTime(time);
        jms.send(cacheUpdateQueue, session -> session.createObjectMessage(new CacheUpdateDTO(account, request)));
    }
}
