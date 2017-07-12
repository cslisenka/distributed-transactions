package com.example.bank.config;

import com.example.bank.api.MoneyTransferRequestListener;
import com.example.bank.integration.partner.sql.PartnerTransferService;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jmx.support.MBeanServerFactoryBean;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.sql.DataSource;

@Configuration
public class LocalConfiguration {

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
    public PartnerTransferService partnerTransferService() {
        return new PartnerTransferService(jdbcTemplate(), partnerJdbcTemplate());
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
    public SimpleMessageListenerContainer requestProcessingContainer(MoneyTransferRequestListener listener) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setDestination(requestQueue());
        container.setMessageListener(listener);
        return container;
    }
}
