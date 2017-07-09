package com.example.bank.api;

import com.example.bank.model.OverdraftException;
import com.example.bank.service.LocalTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

@Component
public class MoneyTransferRequestListener implements MessageListener {

    @Autowired
    private LocalTransferService localTransferService;

    @Override
    public void onMessage(Message message) {
        try {
            // Handle received payment request
            if (message instanceof MapMessage) {
                MapMessage map = (MapMessage) message;

                localTransferService.doTransfer(
                        map.getString("from"), map.getString("to"), map.getInt("amount"));
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        } catch (OverdraftException e) {
            // TODO handle overdraft
            e.printStackTrace();
        }
    }
}