package com.slisenko.examples.test.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class TestClientAcknowledge {

    public static void main(String[] args) throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");

        Connection con = factory.createConnection();
        con.setExceptionListener(e -> e.printStackTrace());
        con.start();

        Session session = con.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        Queue queue = session.createQueue("client.acknowledge.Q");
        Topic topic = session.createTopic("client.acknowledge.T");

        MessageConsumer queueConsumer = session.createConsumer(queue);
        queueConsumer.setMessageListener(message -> {
            System.out.println("Q: " + message);
            try {
                // If we do not acknowledge - we start receiving previous messages again and again
//                message.acknowledge();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // TODO we probably need to configure durable topic
        // TODO it doesn't look like topic is persistent
        // TODO we do not acknowledge, however we are not receiving previous messages again and again
        MessageConsumer topicConsumer = session.createConsumer(topic);
        topicConsumer.setMessageListener(message -> {
            System.out.println("T: " + message);
            try {
//                message.acknowledge();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        MessageProducer queueProducer = session.createProducer(queue);
        MessageProducer topicProducer = session.createProducer(topic);

        // Producing messages
        queueProducer.send(queue, session.createTextMessage("q1"));
        queueProducer.send(queue, session.createTextMessage("q2"));

        topicProducer.send(topic, session.createTextMessage("t1"));
        topicProducer.send(topic, session.createTextMessage("t2"));

        Thread.sleep(5000);

        session.close();
        con.close();
    }
}