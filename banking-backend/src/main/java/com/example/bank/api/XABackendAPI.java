package com.example.bank.api;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.example.bank.model.MoneyTransferDTO;
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
import java.util.UUID;

@RestController
public class XABackendAPI extends AbstractBackendAPI implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(XABackendAPI.class);

    @Autowired
    @Qualifier("xaLocal")
    private JdbcTemplate xaLocal;

    @Autowired
    @Qualifier("xaPartner")
    private JdbcTemplate xaPartner;

    @Autowired
    @Qualifier("xaJms")
    private JmsTemplate xaJms;

    @Autowired
    @Qualifier("xaCacheUpdateQueue")
    private Queue xaCacheUpdateQueue;

    @Autowired
    @Qualifier("xaTransactionManager")
    protected UserTransactionManager tm;

    @PostMapping("/xa/transfer")
    public String xaTransferMoney(@RequestBody MoneyTransferDTO request) throws Exception {
        tm.begin(); // Start JTA transaction
        logger.info("Received WS-request {} transactionId={}", request, tm.getTransaction());
        try {
            String transferID = UUID.randomUUID().toString();
            request.setTransferId(transferID);
            doTransfer(request);
            tm.commit();
            return transferID;
        } catch (Exception e) {
            logger.error("Error processing WS-request {} ({})", request, e.getMessage());
            tm.rollback();
            throw e;
        }
    }

    // We are already in JTA transaction because we are using Atomikos JMS container
    @Override
    public void onMessage(Message message) {
        try {
            MapMessage map = (MapMessage) message;
            MoneyTransferDTO request = MoneyTransferDTO.from(map);
            logger.info("Received JMS-request {} transactionId={}", request, tm.getTransaction());
            doTransfer(request);
        } catch (Exception e) {
            logger.error("Error processing JMS-request {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected JdbcTemplate local() {
        return xaLocal;
    }

    @Override
    protected JdbcTemplate partner() {
        return xaPartner;
    }

    @Override
    protected JmsTemplate jms() {
        return xaJms;
    }

    @Override
    protected Queue cacheUpdateQueue() {
        return xaCacheUpdateQueue;
    }

    @Override
    protected boolean isXA() {
        return true;
    }
}