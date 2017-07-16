package com.slisenko.examples.test.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * http://activemq.apache.org/hello-world.html
 */
public class TestActiveMQ {

    public static ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void main(String[] args) throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");

        Connection con = factory.createConnection();
        con.setExceptionListener(e -> e.printStackTrace());
        con.start();

        Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        session.createDurableSubscriber()// TODO what is the difference between durable subsctiber and consumer?
        // TODO is durable subscriber only works for topics?

        Destination destination = session.createQueue("TEST.QUEUE");
        MessageProducer producer = session.createProducer(destination);
        // producer.setDeliveryMode(); // TODO what is delivery mode?
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(message -> System.out.println(message));

        producer.send(session.createTextMessage("hello"));

        // Can we use session.commit/rollback only when sending messages?

        con.close();
    }
}