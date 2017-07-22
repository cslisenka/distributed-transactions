package com.example.bank.api;

import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactory;
import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactoryImpl;
import ch.maxant.generic_jca_adapter.TransactionAssistant;
import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.example.bank.integration.partner.HTTPTransferService;
import com.example.bank.model.*;
import com.example.bank.model.mapper.AccountRowMapper;
import com.example.bank.model.mapper.MoneyTransferRowMapper;
import com.example.bank.model.mapper.PartnerMoneyTransferRowMapper;
import com.example.bank.integration.partner.SQLTransferService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.HazelcastXAResource;
import com.hazelcast.transaction.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import javax.jms.MapMessage;
import javax.jms.Queue;
import javax.transaction.*;
import javax.transaction.xa.XAResource;
import java.util.*;

@RestController
public class MoneyTransferAPI {

    private static final Logger logger = LoggerFactory.getLogger(MoneyTransferAPI.class);
    public static final String PENDING_TRANSFERS = "pendingTransfers";

    @Autowired
    private HazelcastInstance hazelcast;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private LocalTransferService localTransferService;

    @Autowired
    @Qualifier("partnerTransferService")
    private SQLTransferService sqlTransferService;

    @Autowired
    @Qualifier("xaPartnerTransferService")
    private SQLTransferService xaSQLTransferService;

    @Autowired
    private HTTPTransferService httpTransferService;

    @Autowired
    @Qualifier("localJdbc")
    private JdbcTemplate jdbc;

    @Autowired
    private Queue requestQueue;

    @Autowired
    @Qualifier("xaJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired
    private UserTransactionManager txManager;

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

    @GetMapping("/pendingTransfers")
    public Collection<Object> getPendingTransfers() {
        return hazelcast.getMap(PENDING_TRANSFERS).values();
    }

    @PostMapping("/transferMoneyLocal")
    public void transferMoney(@RequestBody Map<String, String> request) throws OverdraftException {
        // TODO integrate with Hazelcast
        String transferId = UUID.randomUUID().toString();
        localTransferService.doTransfer(transferId, request.get("from"),
                request.get("to"), Integer.parseInt(request.get("amount")));
    }

    @PostMapping("/transferMoneyToPartner")
    public void transferMoneyToPartner(@RequestBody Map<String, String> request) throws OverdraftException {
        // TODO integrate with Hazelcast
        String transferId = UUID.randomUUID().toString();
        // As we are writing directly to the partners database, we are responsible for generating partner transfer IDs
        String partnerTransferId = UUID.randomUUID().toString();
        sqlTransferService.doTransfer(transferId, request.get("from"),
                partnerTransferId, request.get("to"), Integer.parseInt(request.get("amount")));
    }

    @PostMapping("/xaTransferMoneyToPartnerSQL")
    public void xaTransferMoneyToPartner(@RequestBody Map<String, String> request) throws OverdraftException, SystemException, HeuristicRollbackException, HeuristicMixedException, RollbackException, NotSupportedException {
        txManager.begin(); // JTA transaction
        try {
            String transferId = UUID.randomUUID().toString();
            String partnerTransferId = UUID.randomUUID().toString();
            xaSQLTransferService.doTransfer(transferId, request.get("from"),
                    partnerTransferId, request.get("to"), Integer.parseInt(request.get("amount")));
            txManager.commit();
        } catch (Exception e) {
            txManager.rollback();
            throw e;
        }
    }

    @PostMapping("/xaTransferMoneyToPartnerWS")
    public void xaTransferMoneyToPartnerWS(@RequestBody Map<String, String> request) throws Exception {
        // If transaction is already started higher (in JMS listener) - the code must be commented
        // If not commented - this method will be executed within nested transaction
        // TODO change to txManager
//        UserTransaction tx = context.getBean(UserTransactionImp.class); // JTA transaction
//        tx.begin();
        try {

            // For demonstration reasons we can do request with account out of money or non-existing account (if SQL executed after WS-call - in this case TM triggers WS-reject)
            // Doing local transfer
            String transferId = UUID.randomUUID().toString();
//            xaSQLTransferService.doTransferLocal(transferId, request.get("from"),
//                    partnerTransferId, request.get("to"), Integer.parseInt(request.get("amount")));
            xaSQLTransferService.doTransferLocal(transferId, request.get("from"),
                    "none", request.get("to"), Integer.parseInt(request.get("amount")));

            String partnerTransferId = null;

            // For demonstration purposes we can shut down partner web-services and show that transaction was rolled back (if our SQL is first)
            // If our database (next operation) rejects payment - the cancel should be called on web-service
            // TransactionManager will hold XA transaction on MySQL side and repeat retries to WS-confirm until timeout is passed, then call WS-cancal
            BasicTransactionAssistanceFactory transferServiceTransactionFactory = new BasicTransactionAssistanceFactoryImpl("xa/transferService"); // TODO how to avoid using JNDI?
            try (TransactionAssistant transactionAssistant = transferServiceTransactionFactory.getTransactionAssistant()){
                partnerTransferId = transactionAssistant.executeInActiveTransaction(xaTransactionId -> {
                    // In case of failure - TransactionManager must call WS-reject
                    return httpTransferService.reserveMoney(request.get("from"), request.get("to"), Integer.parseInt(request.get("amount")), xaTransactionId);
                });
            }

//            tx.commit();
        } catch (Exception e) {
            logger.error("Failed XA (DB+WS) transfer, exception={}", e.getMessage());
//            tx.rollback();
            throw e;
        }

        // TODO add web-page for visualizing recovery logs for Atomikos and our WS XA adapter - just print all files we have
    }

    @PostMapping("/queuedTransferMoney")
    public String queueMoneyTransfer(@RequestBody Map<String, String> request) throws SystemException, NotSupportedException {
        txManager.begin();
        Transaction transaction = txManager.getTransaction();

        try {
            // This may be needed for high performance as well if we want to temporary not accept new payments for DB maintenance
            String transferId = UUID.randomUUID().toString(); // UUID to make request idempotent

            HazelcastXAResource hazelcastXA = hazelcast.getXAResource();
            transaction.enlistResource(hazelcastXA);

            TransactionContext hazelcastTx = hazelcastXA.getTransactionContext();
            TransactionalMap<String, PartnerMoneyTransfer> pendingTransfers = hazelcastTx.getMap(PENDING_TRANSFERS);
            pendingTransfers.put(transferId, new PartnerMoneyTransfer(transferId, request.get("from"),
                    request.get("to"), Integer.parseInt(request.get("amount")), PartnerMoneyTransfer.Direction.OUT));

            jmsTemplate.send(requestQueue, session -> {
                MapMessage message = session.createMapMessage();
                message.setString("transfer_id", transferId);
                message.setString("from", request.get("from"));
                message.setString("to", request.get("to"));
                message.setInt("amount", Integer.parseInt(request.get("amount")));
                return message;
            });

            // TODO when should we delist resource? Just before commit, or after we completed all operations with him?
            // TODO check XA specification
            transaction.delistResource(hazelcastXA, XAResource.TMSUCCESS);
            transaction.commit();
            // TODO return status: success, error
            return transferId; // We can add payment ID to cache for displaying to user, or just return it to frontend
        } catch (Exception e) {
            transaction.rollback();
            return "ERROR";
        }
    }
}