package com.example.bank;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.example.bank.model.AccountDTO;
import com.example.bank.model.Constants;
import com.example.bank.model.MoneyTransferDTO;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.HazelcastXAResource;
import com.hazelcast.transaction.TransactionContext;
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

@RestController
public class FrontEndAPI implements MessageListener {

    @Autowired
    private HazelcastInstance cache;

    @Autowired
    private JmsTemplate jms;

    @Autowired
    private Queue moneyTransferQueue;

    @Autowired
    private UserTransactionManager tm;

    @GetMapping("/account")
    public List<AccountAndTransfers> getAllAccounts() {
        List<AccountAndTransfers> response = new ArrayList<>();

        Map<String, AccountDTO> accounts = cache.getMap(Constants.HAZELCAST_ACCOUNTS);
        for (AccountDTO account : accounts.values()) {
            Map<String, MoneyTransferDTO> moneyTransfers = cache.getMap(Constants.HAZELCAST_TRANSFERS + "_" + account.getIdentifier());
            Map<String, MoneyTransferDTO> pendingMoneyTransfers = cache.getMap(Constants.HAZELCAST_PENDING_TRANSFERS + "_" + account.getIdentifier());
            response.add(new AccountAndTransfers(account, moneyTransfers.values(), pendingMoneyTransfers.values()));
        }

        return response;
    }

    @PostMapping("/transfer")
    public String queueMoneyTransfer(@RequestBody MoneyTransferDTO request) throws SystemException, NotSupportedException {
        tm.begin();
        Transaction tx = tm.getTransaction();
        try {
            // This may be needed for high performance as well if we want to temporary not accept new payments for DB maintenance
            String transferId = UUID.randomUUID().toString(); // UUID to make request idempotent
            request.setTransferId(transferId);
            request.setDateTime(new Date());

            // Send JMS message
            jms.send(moneyTransferQueue, session -> {
                MapMessage message = session.createMapMessage();
                request.copyTo(message);
                return message;
            });

            // Update cache
            HazelcastXAResource xaCache = cache.getXAResource();
            tx.enlistResource(xaCache);
            TransactionContext cacheTx = xaCache.getTransactionContext();
            TransactionalMap<String, AccountDTO> accountMap = cacheTx.getMap(Constants.HAZELCAST_ACCOUNTS);
            TransactionalMap<String, MoneyTransferDTO> pendingTransfersMap = cacheTx.getMap(Constants.HAZELCAST_PENDING_TRANSFERS + "_" + request.getFrom());

            AccountDTO account = accountMap.getForUpdate(request.getFrom());
            if (account != null) {
                account.setBalance(account.getBalance() - request.getAmount()); // TODO add overdraft check if we get negative balance here
                accountMap.replace(account.getIdentifier(), account);
                pendingTransfersMap.put(transferId, request);
            } else {
                throw new RuntimeException("Non-existing account " + request.getFrom());
            }

            // TODO when should we delist resource? Just before commit, or after we completed all operations with him?
            // TODO check XA specification
            tx.delistResource(xaCache, XAResource.TMSUCCESS);
            tx.commit();
            return transferId;
        } catch (Exception e) {
            tx.rollback();
            return e.getMessage();
        }
    }

    @Override
    public void onMessage(Message message) {
        // TODO handle message and update cache
        System.out.println(message);
    }

    static public class AccountAndTransfers {

        private AccountDTO account;
        private Collection<MoneyTransferDTO> completedTransfers;
        private Collection<MoneyTransferDTO> pendingTransfers;

        public AccountAndTransfers(AccountDTO account, Collection<MoneyTransferDTO> completedTransfers,
                                   Collection<MoneyTransferDTO> pendingTransfers) {
            this.account = account;
            this.completedTransfers = completedTransfers;
            this.pendingTransfers = pendingTransfers;
        }

        public AccountDTO getAccount() {
            return account;
        }

        public Collection<MoneyTransferDTO> getCompletedTransfers() {
            return completedTransfers;
        }

        public Collection<MoneyTransferDTO> getPendingTransfers() {
            return pendingTransfers;
        }
    }
}