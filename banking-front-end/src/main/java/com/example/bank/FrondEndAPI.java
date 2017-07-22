package com.example.bank;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.example.bank.model.CachedAccount;
import com.example.bank.model.CachedMoneyTransfer;
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
import javax.jms.Queue;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import java.util.*;

@RestController
public class FrondEndAPI {

    public static final String HAZELCAST_ACCOUNTS = "account";
    public static final String HAZELCAST_TRANSFERS = "transfer";

    @Autowired
    private HazelcastInstance hazelcast;

    @Autowired
    private JmsTemplate jms;

    @Autowired
    private Queue moneyTransferQueue;

    @Autowired
    private UserTransactionManager tm;

    @GetMapping("/account")
    public List<AccountAndTransfers> getAllAccounts() {
        List<AccountAndTransfers> response = new ArrayList<>();

        Map<String, CachedAccount> accounts = hazelcast.getMap(HAZELCAST_ACCOUNTS);
        for (CachedAccount account : accounts.values()) {
            Map<String, CachedMoneyTransfer> moneyTransfers = hazelcast.getMap(HAZELCAST_TRANSFERS + "_" + account.getIdentifier());
            response.add(new AccountAndTransfers(account, moneyTransfers.values()));
        }

        return response;
    }

    @PostMapping("/transfer")
    public String queueMoneyTransfer(@RequestBody Map<String, String> request) throws SystemException, NotSupportedException {
        String from = request.get("from");
        String to = request.get("to");
        Integer amount = Integer.parseInt(request.get("amount"));

        tm.begin();
        Transaction tx = tm.getTransaction();

        try {
            // This may be needed for high performance as well if we want to temporary not accept new payments for DB maintenance
            String transferId = UUID.randomUUID().toString(); // UUID to make request idempotent

            // Update cache
            HazelcastXAResource hazelcastXA = hazelcast.getXAResource();
            tx.enlistResource(hazelcastXA);
            TransactionContext hazelcastTX = hazelcastXA.getTransactionContext();
            TransactionalMap<String, CachedAccount> accounts = hazelcastTX.getMap(HAZELCAST_ACCOUNTS);
            CachedAccount account = accounts.getForUpdate(from);
            if (account != null) {
                account.setBalance(account.getBalance() - amount); // TODO add overdraft check if we get negative balance here

                hazelcastTX.getMap(HAZELCAST_TRANSFERS + "_" + account.getIdentifier())
                    .put(transferId, new CachedMoneyTransfer(transferId, from, to, amount, CachedMoneyTransfer.Direction.OUT)); // TODO set status as pending
            }

            // Send JMS message
            jms.send(moneyTransferQueue, session -> {
                MapMessage message = session.createMapMessage();
                message.setString("transfer_id", transferId);
                message.setString("from", from);
                message.setString("to", to);
                message.setInt("amount", amount);
                return message;
            });

            // TODO when should we delist resource? Just before commit, or after we completed all operations with him?
            // TODO check XA specification
            tx.delistResource(hazelcastXA, XAResource.TMSUCCESS);
            tx.commit();
            // TODO return status: success, error
            return transferId; // We can add payment ID to cache for displaying to user, or just return it to frontend
        } catch (Exception e) {
            tx.rollback();
            return "ERROR";
        }
    }

     static public class AccountAndTransfers {

        private CachedAccount account;
        private Collection<CachedMoneyTransfer> transfers;

        public AccountAndTransfers(CachedAccount account, Collection<CachedMoneyTransfer> transfers) {
            this.account = account;
            this.transfers = transfers;
        }

        public CachedAccount getAccount() {
            return account;
        }

        public Collection<CachedMoneyTransfer> getTransfers() {
            return transfers;
        }
    }
}