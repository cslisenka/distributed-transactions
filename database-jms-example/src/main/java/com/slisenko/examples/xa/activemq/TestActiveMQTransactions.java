package com.slisenko.examples.xa.activemq;

import com.slisenko.examples.test.TestActiveMQ;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class TestActiveMQTransactions {

    public static void main(String[] args) throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection con = factory.createConnection();
        con.setExceptionListener(new MyExceptionListener());
        con.start();

        // Producing
        Session session = con.createSession(true, Session.SESSION_TRANSACTED);
        Destination destination = session.createQueue("QUEUE.JMS.TRANSACTIONS");
        MessageProducer producer = session.createProducer(destination);

        producer.send(session.createTextMessage("not send this"));
        session.rollback(); // Should not go

        producer.send(session.createTextMessage("success " + System.currentTimeMillis()));
        session.commit(); // Should go
        session.close();

        // Consuming
        Session session2 = con.createSession(true, Session.SESSION_TRANSACTED);
        MessageConsumer consumer = session2.createConsumer(destination);

        System.out.println("Received: " + consumer.receive());
        session2.rollback(); // Return message back

        System.out.println("Received again: " + consumer.receive());
        session2.commit(); // Take message forever

        System.out.println("No other messages: " + consumer.receiveNoWait()); // No other messages
        session2.close();
    }

    static class MyExceptionListener implements ExceptionListener {

        @Override
        public void onException(JMSException e) {
            e.printStackTrace();
        }
    }
}