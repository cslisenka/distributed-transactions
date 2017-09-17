package com.example.travel.client;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jms.extra.MessageDrivenContainer;
import com.example.travel.model.Constants;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jta.atomikos.AtomikosConnectionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Queue;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import java.io.File;
import java.io.IOException;

@Configuration
@SpringBootApplication
public class TravelClientMain {

	private static final File TX_LOG_PATH = new File("C:\\Work\\JAVA tech talks\\Distributed transactions\\java\\distributed-transactions\\travel-client\\transaction_log");

	public static void main(String[] args) throws IOException {
		// Clean atomikos logs
		FileUtils.cleanDirectory(TX_LOG_PATH);

		SpringApplication.run(TravelClientMain.class, args);
	}

	@Bean("requestQueue")
	public Queue requestQueue() {
		return new ActiveMQQueue(Constants.QUEUE_XA_BOOKING_REQUEST);
	}

    @Bean("responseQueue")
    public Queue responseQueue() {
        return new ActiveMQQueue(Constants.QUEUE_XA_BOOKING_RESPONSE);
    }

	@Primary // To override default spring boot transaction manager
	@Bean(initMethod = "init", destroyMethod = "close")
	public UserTransactionManager xaTransactionManager() throws SystemException {
		UserTransactionManager txManager = new UserTransactionManager();
		txManager.setTransactionTimeout(300);
		txManager.setForceShutdown(false);
		return txManager;
	}

    @Autowired
    @Bean(initMethod = "start", destroyMethod = "stop")
    public MessageDrivenContainer responseJMSContainer(TravelClientAPI listener) {
        MessageDrivenContainer container = new MessageDrivenContainer();
        container.setAtomikosConnectionFactoryBean(atomikosActiveMQ());
        container.setTransactionTimeout(100);
        container.setDestination(responseQueue());
        container.setMessageListener(listener);
        return container;
    }

	@Bean(destroyMethod = "shutdown")
	public HazelcastInstance hazelcast() {
		Config config = new Config();
		config.getNetworkConfig()
				.setPort(5701)
				.setPortAutoIncrement(false); // Should be true in case of cluster
//            .setPortCount(20); // Ports for cluster members

		config.getNetworkConfig().getJoin().getMulticastConfig()
				.setEnabled(false);
		// TODO configure management center application

		return Hazelcast.newHazelcastInstance(config);
	}

	@Bean
	public JmsTemplate jmsTemplate() {
		JmsTemplate template = new JmsTemplate(atomikosActiveMQ());
		template.setSessionTransacted(true); // TODO probably doesn't make sense for Atomimkos, only needed if nonxa tx used
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

	@Primary
	@Bean
	public DataSource database() {
		MysqlDataSource db = new MysqlDataSource();
		db.setCreateDatabaseIfNotExist(true);
		db.setPort(3306);
		db.setUser("root");
		db.setPassword("root");
		db.setServerName("localhost");
		db.setDatabaseName("travel_agency");
		return db;
	}
}