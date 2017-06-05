package com.slisenko.examples;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

/**
 * http://activemq.apache.org/hello-world.html
 */
public class TestActiveMQ {

    public static void main(String[] args) throws JMSException, InterruptedException {
        System.out.println("Test ActiveMQ");

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");

        Connection con = factory.createConnection();
        con.setExceptionListener(new Listener());
        con.start();

        Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue("TEST.QUEUE");
        MessageProducer producer = session.createProducer(destination);
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(new Listener());

        producer.send(session.createTextMessage("hello"));
    }

    static class Listener implements ExceptionListener, MessageListener {

        @Override
        public void onException(JMSException e) {
            e.printStackTrace();
        }

        @Override
        public void onMessage(Message message) {
            System.out.println("JMS " + message);
        }
    }
}