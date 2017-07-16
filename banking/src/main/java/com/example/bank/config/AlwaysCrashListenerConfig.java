package com.example.bank.config;

import com.atomikos.jms.AtomikosConnectionFactoryBean;
import com.atomikos.jms.extra.MessageDrivenContainer;
import com.example.bank.api.AlwaysCrashListener;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;

//@RestController
//@Configuration
public class AlwaysCrashListenerConfig {

    @Autowired
    @Qualifier("local")
    private JmsTemplate jms;

    @GetMapping("crashAtomikos")
    public void crashAtomikos() {
        System.out.println("crashAtomikos");
        jms.send(alwaysCrashQueue(), session -> session.createTextMessage("1"));
    }

    @GetMapping("crashAmq")
    public void crashAmq() {
        System.out.println("crashAmq");
        jms.send(alwaysCrashQueue2(), session -> session.createTextMessage("1"));
    }

    @Bean
    public Queue alwaysCrashQueue() {
        return new ActiveMQQueue("ALWAYS.CRASH.ATOMIKOS");
    }

    @Bean
    public Queue alwaysCrashQueue2() {
        return new ActiveMQQueue("ALWAYS.CRASH.SESSION.TRANSACTED");
    }

//    @Autowired
//    @Bean(name = "atomikosAlwaysCrash", initMethod = "start", destroyMethod = "stop")
//    public MessageDrivenContainer requestProcessingContainer(AlwaysCrashListener listener, AtomikosConnectionFactoryBean amq) {
//        MessageDrivenContainer container = new MessageDrivenContainer();
//        container.setAtomikosConnectionFactoryBean(amq);
//        container.setTransactionTimeout(100);
//        container.setDestination(alwaysCrashQueue());
//        container.setMessageListener(listener);
//        return container;
//    }

    @Autowired
    @Bean("amqAlwaysCrash")
    public SimpleMessageListenerContainer requestProcessingContainer(AlwaysCrashListener listener,
                                                                     @Qualifier("connectionFactory") ActiveMQConnectionFactory amq) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(amq);
        container.setDestination(alwaysCrashQueue2());
        container.setMessageListener(listener);
        container.setSessionTransacted(true); // Not XA
        return container;
    }
}