package com.example.bank;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jta.atomikos.AtomikosConnectionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Queue;
import javax.transaction.SystemException;

@Configuration
@SpringBootApplication
public class FrontEndMain {

	public static void main(String[] args) {
		SpringApplication.run(FrontEndMain.class, args);
	}

	@Bean("requestQueue")
	public Queue requestQueue() {
		return new ActiveMQQueue("MONEY.TRANSFER.QUEUE");
	}

	@Primary // To override default spring boot transaction manager
	@Bean(initMethod = "init", destroyMethod = "close")
	public UserTransactionManager xaTransactionManager() throws SystemException {
		UserTransactionManager txManager = new UserTransactionManager();
		txManager.setTransactionTimeout(300);
		txManager.setForceShutdown(false);
		return txManager;
	}

	@Bean
	public HazelcastInstance hazelcast() {
		ClientConfig config = new ClientConfig();
		config.getNetworkConfig().addAddress("localhost");
		return HazelcastClient.newHazelcastClient(config);
	}

	@Bean
	public JmsTemplate jmsTemplate() {
		JmsTemplate template = new JmsTemplate(atomikosActiveMQ());
		template.setSessionTransacted(true); // TODO probably doesn't make sense for Atomimkos, only needed if local tx used
		return template;
	}

	@Primary // For spring boot autoconfig
	@Bean(initMethod = "init", destroyMethod = "close")
	public AtomikosConnectionFactoryBean atomikosActiveMQ() {
		AtomikosConnectionFactoryBean ds = new AtomikosConnectionFactoryBean();
		ds.setUniqueResourceName("activemq");
		ds.setMaxPoolSize(10);
		ds.setMinPoolSize(5);
		ds.setXaConnectionFactory(activeMqXAConnectionFactory());
		return ds;
	}

	@Bean
	public ActiveMQXAConnectionFactory activeMqXAConnectionFactory() {
		ActiveMQXAConnectionFactory amq = new ActiveMQXAConnectionFactory("tcp://localhost:61616");
		RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
		redeliveryPolicy.setMaximumRedeliveries(5);
		redeliveryPolicy.setInitialRedeliveryDelay(500); // 5 seconds redelivery delay
		redeliveryPolicy.setBackOffMultiplier(2);
		redeliveryPolicy.setUseExponentialBackOff(true);

		amq.setRedeliveryPolicy(redeliveryPolicy);

		// TODO define policy for specific queues
//        RedeliveryPolicyMap map = new RedeliveryPolicyMap();
//        map.setRedeliveryPolicyEntries();

		return amq;
	}
}