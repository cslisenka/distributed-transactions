package com.slisenko.examples.test.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class TestTransactions {

    public static void main(String[] args) throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");

        Connection con = factory.createConnection();
        con.setExceptionListener(e -> e.printStackTrace());
        con.start();

        // Consuming all
        {
            Session consumingSession = con.createSession(true, Session.SESSION_TRANSACTED);

            Queue queue = consumingSession.createQueue("transacted.Q");
            Topic topic = consumingSession.createTopic("transacted.T");

            MessageConsumer queueConsumer = consumingSession.createConsumer(queue);
            queueConsumer.setMessageListener(message -> {
                try {
                    System.out.println("Q: " + ((TextMessage) message).getText() + " " + message);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            MessageConsumer topicConsumer = consumingSession.createConsumer(topic);
            topicConsumer.setMessageListener(message -> {
                try {
                    System.out.println("T: " + ((TextMessage) message).getText() + " " + message);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // And sending something else
            MessageProducer queueProducer = consumingSession.createProducer(queue);
            MessageProducer topicProducer = consumingSession.createProducer(topic);

            queueProducer.send(queue, consumingSession.createTextMessage("consuming-q"));
            topicProducer.send(topic, consumingSession.createTextMessage("consuming-t"));

            // If we comment it out, or call rollback() - we will receive all previous messages again and again every time
            consumingSession.commit();
//            consumingSession.rollback();
            consumingSession.close();
        }

        {
            Session producingSession = con.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = producingSession.createQueue("transacted.Q");
            Topic topic = producingSession.createTopic("transacted.T");

            MessageProducer queueProducer = producingSession.createProducer(queue);
            MessageProducer topicProducer = producingSession.createProducer(topic);

            // Producing messages, then rollback
            queueProducer.send(queue, producingSession.createTextMessage("rollback-q1"));
            queueProducer.send(queue, producingSession.createTextMessage("rollback-q2"));

            topicProducer.send(topic, producingSession.createTextMessage("rollback-t1"));
            topicProducer.send(topic, producingSession.createTextMessage("rollback-t2"));
            producingSession.rollback(); // Can we reuse the same Session after rollback()?

            // Producing messages, then commit
            queueProducer.send(queue, producingSession.createTextMessage("commit-q1"));
            queueProducer.send(queue, producingSession.createTextMessage("commit-q2"));

            topicProducer.send(topic, producingSession.createTextMessage("commit-t1"));
            topicProducer.send(topic, producingSession.createTextMessage("commit-t2"));
            producingSession.commit();
            producingSession.close();
        }

        Thread.sleep(3000);
        con.close();
    }
}