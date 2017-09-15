package com.example.bank;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.example.bank.model.AccountDTO;
import com.example.bank.model.Constants;
import com.example.bank.model.MoneyTransferDTO;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.HazelcastXAResource;
import com.hazelcast.transaction.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import java.util.*;
import java.util.function.Consumer;

@RestController
public class FrontEndAPI implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(CacheLoader.class);

    @Autowired
    private HazelcastInstance cache;

    @Autowired
    private JmsTemplate jms;

    @Autowired
    private Queue moneyTransferQueue;

    @Autowired
    private UserTransactionManager tm;

    @GetMapping("/account")
    public List<AccountAndTransfersDTO> getAllAccounts() {
        List<AccountAndTransfersDTO> response = new ArrayList<>();

        Map<String, AccountDTO> accounts = cache.getMap(Constants.HAZELCAST_ACCOUNTS);
        for (AccountDTO account : accounts.values()) {
            Map<String, MoneyTransferDTO> transfers = cache.getMap(Constants.HAZELCAST_TRANSFERS + "_" + account.getIdentifier());
            Map<String, MoneyTransferDTO> pendingTransfers = cache.getMap(Constants.HAZELCAST_PENDING_TRANSFERS + "_" + account.getIdentifier());
            response.add(new AccountAndTransfersDTO(account, transfers.values(), pendingTransfers.values()));
        }

        return response;
    }

    @PostMapping("/transfer")
    public String queueMoneyTransfer(@RequestBody MoneyTransferDTO request) throws SystemException, NotSupportedException {
        tm.begin();
        try {
            // This may be needed for high performance as well if we want to temporary not accept new payments for DB maintenance
            String transferId = UUID.randomUUID().toString(); // UUID to make request idempotent
            request.setTransferId(transferId);
            request.setDateTime(new Date());

            // Send JMS message
            jms.send(moneyTransferQueue,
                session -> request.to(session.createMapMessage())
            );

            // Update cache
            doInTransaction(cacheTx -> {
                TransactionalMap<String, AccountDTO> accounts = cacheTx.getMap(Constants.HAZELCAST_ACCOUNTS);
                cacheTx.getMap(Constants.HAZELCAST_PENDING_TRANSFERS + "_" + request.getFrom())
                    .put(transferId, request);

                AccountDTO account = accounts.getForUpdate(request.getFrom());
                if (account != null) {
                    account.setBalance(account.getBalance() - request.getAmount()); // TODO add overdraft check if we get negative balance here
                    accounts.replace(account.getIdentifier(), account);
                } else {
                    throw new RuntimeException("Non-existing account " + request.getFrom());
                }
            });

            logger.info("Sending {} to queue", request);

            tm.commit();
            return transferId;
        } catch (Exception e) {
            tm.rollback();
            logger.error("Error sending {} ({})", request, e.getMessage());
            return e.getMessage();
        }
    }

    // Should be already in XA transaction
    @Override
    public void onMessage(Message message) {
        // TODO handle if we had money  transfer error
        MapMessage map = (MapMessage) message;
        try {
            AccountDTO account = AccountDTO.from(map);
            MoneyTransferDTO transfer = MoneyTransferDTO.from(map);

            // Update cache with new account and new money transfer
            doInTransaction(cacheTx -> {
                // Update account
                cacheTx.getMap(Constants.HAZELCAST_ACCOUNTS).replace(account.getIdentifier(), account);
                // Remove pending transfer
                cacheTx.getMap(Constants.HAZELCAST_PENDING_TRANSFERS + "_" + account.getIdentifier()).remove(transfer.getTransferId());
                // Add permanent transfer
                // TODO handle if we got error for transfer
                cacheTx.getMap(Constants.HAZELCAST_TRANSFERS + "_" + account.getIdentifier()).put(transfer.getTransferId(), transfer);
            });

            logger.info("Updated cache {}, {}", account, transfer);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error updating cache {} ({})", message, e.getMessage());
            throw new RuntimeException(e); // Cause transaction rollback
        }
    }

    private void doInTransaction(Consumer<TransactionContext> action) throws Exception {
        Transaction tx = tm.getTransaction();
        HazelcastXAResource xaCacheResource = cache.getXAResource();
        tx.enlistResource(xaCacheResource);
        TransactionContext cacheTx = xaCacheResource.getTransactionContext();

        // Executing business logic
        action.accept(cacheTx);

        // TODO when should we delist resource? Just before commit, or after we completed all operations with him?
        // TODO check XA specification
        tx.delistResource(xaCacheResource, XAResource.TMSUCCESS);
    }

    static public class AccountAndTransfersDTO {

        private AccountDTO account;
        private Collection<MoneyTransferDTO> completed;
        private Collection<MoneyTransferDTO> pending;

        public AccountAndTransfersDTO(AccountDTO account, Collection<MoneyTransferDTO> completed,
                                      Collection<MoneyTransferDTO> pending) {
            this.account = account;
            this.completed = completed;
            this.pending = pending;
        }

        public AccountDTO getAccount() {
            return account;
        }

        public Collection<MoneyTransferDTO> getCompleted() {
            return completed;
        }

        public Collection<MoneyTransferDTO> getPending() {
            return pending;
        }
    }
}