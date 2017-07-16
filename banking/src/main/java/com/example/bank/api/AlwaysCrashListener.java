package com.example.bank.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import javax.jms.MessageListener;

// Listener always throws RuntimeException
// In this case ActiveMQ transaction always rollbacks and having 1 message should trigger infinite number of calls
// TODO that's may be active MQ limit for redelivery count = 7
@Component
public class AlwaysCrashListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(AlwaysCrashListener.class);

    @Override
    public void onMessage(Message message) {
        logger.info("RECEIVED " + message);
        throw new RuntimeException("test exception");
    }
}
