package com.example.bank.config;

import com.example.bank.integration.partner.SQLTransferService;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.sql.DataSource;

@Configuration
public class LocalConfiguration {

    @Bean
    public HazelcastInstance hazelcast() {
        ClientConfig config = new ClientConfig();
        config.getNetworkConfig().addAddress("localhost");

        return HazelcastClient.newHazelcastClient(config);
    }

    @Bean("localJdbc")
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(localDataSource());
    }

    @Bean("partnerJdbc")
    public JdbcTemplate partnerJdbcTemplate() {
        return new JdbcTemplate(partnerDataSource());
    }

    @Bean(name = "partnerDataSource")
    public DataSource partnerDataSource() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setPort(3306);
        dataSource.setServerName("localhost");
        dataSource.setUser("root");
        dataSource.setPassword("root");
        dataSource.setDatabaseName("partner_bank");
        return dataSource;
    }

    @Primary
    @Bean("localDataSource")
    public DataSource localDataSource() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setPort(3306);
        dataSource.setServerName("localhost");
        dataSource.setUser("root");
        dataSource.setPassword("root");
        dataSource.setDatabaseName("bank1");
        return dataSource;
    }

    @Bean("partnerTransferService")
    public SQLTransferService partnerTransferService() {
        return new SQLTransferService(jdbcTemplate(), partnerJdbcTemplate());
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory amq = new ActiveMQConnectionFactory("tcp://localhost:61616");

        // If messages were failed to be redelivered, they go to ActiveMQ.DLQ queue
        // The queue name can be changed (we can define rules routing messages to different queues)
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(10);
        redeliveryPolicy.setInitialRedeliveryDelay(500); // 5 seconds redelivery delay
        redeliveryPolicy.setBackOffMultiplier(2);
        redeliveryPolicy.setUseExponentialBackOff(true);

        amq.setRedeliveryPolicy(redeliveryPolicy);
        return amq;
    }

    @Bean("requestQueue")
    public Queue requestQueue() {
        return new ActiveMQQueue("MONEY.TRANSFER.REQUESTS");
    }

    @Bean("local")
    public JmsTemplate jmsTemplate() {
        return new JmsTemplate(connectionFactory());
    }

//    @Autowired
//    @Bean
//    public SimpleMessageListenerContainer requestProcessingContainer(MoneyTransferRequestListener listener) {
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory());
//        container.setDestination(requestQueue());
//        container.setMessageListener(listener);
//        container.setSessionTransacted(true); // Not XA
//        return container;
//    }
}
