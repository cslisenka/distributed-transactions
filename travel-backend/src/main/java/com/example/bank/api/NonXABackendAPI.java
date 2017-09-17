package com.example.bank.api;

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
public class NonXABackendAPI extends AbstractBackendAPI implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(NonXABackendAPI.class);

    @Autowired
    @Qualifier("local")
    protected JdbcTemplate local;

    @Autowired
    @Qualifier("partner")
    protected JdbcTemplate partner;

    @Autowired
    @Qualifier("jms")
    protected JmsTemplate jms;

    @Autowired
    @Qualifier("cacheUpdateQueue")
    private Queue cacheUpdateQueue;

    @PostMapping("/transfer")
    public String transferMoney(@RequestBody MoneyTransferDTO request) throws Exception {
        logger.info("Received WS-request {}", request);
        try {
            String transferID = UUID.randomUUID().toString();
            request.setTransferId(transferID);
//            doTransfer(request);
            return transferID;
        } catch (Exception e) {
            logger.error("Error processing WS-request {} ({})", request, e.getMessage());
            throw e;
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            MapMessage map = (MapMessage) message;
            MoneyTransferDTO request = MoneyTransferDTO.from(map);
            logger.info("Received JMS-request {}", request);
//            doTransfer(request);
        } catch (Exception e) {
            logger.error("Error processing JMS-request {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}