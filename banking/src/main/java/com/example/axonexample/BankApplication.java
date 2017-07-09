package com.example.axonexample;

import com.example.axonexample.api.MoneyTransferRequestListener;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

@Configuration
@SpringBootApplication
public class BankApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankApplication.class, args);
	}

	@Bean
	public ConnectionFactory connectionFactory() {
		return new ActiveMQConnectionFactory("tcp://localhost:61616");
	}

	@Bean
	public Queue requestQueue() {
	    return new ActiveMQQueue("MONEY.TRANSFER.REQUESTS");
    }

    @Bean
    public JmsTemplate jmsTemplate() {
	    return new JmsTemplate(connectionFactory());
    }

    @Autowired
    @Bean
    public SimpleMessageListenerContainer container(MoneyTransferRequestListener listener) {
	    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
	    container.setConnectionFactory(connectionFactory());
	    container.setDestination(requestQueue());
	    container.setMessageListener(listener);
	    return container;
    }
}