package com.example.bank.config;

import ch.maxant.generic_jca_adapter.CommitRollbackCallback;
import ch.maxant.generic_jca_adapter.MicroserviceXAResource;
import ch.maxant.generic_jca_adapter.TransactionConfigurator;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.example.bank.integration.partner.HTTPTransferService;
import com.example.bank.integration.partner.SQLTransferService;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jta.atomikos.AtomikosConnectionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.RestTemplate;

import javax.sql.XADataSource;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.io.File;

@Configuration
public class XAConfiguration {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Autowired
    public CommitRollbackCallback partnerMoneyTransfer(HTTPTransferService httpTransferService) {
        CommitRollbackCallback callback = new CommitRollbackCallback() {
            @Override
            public void commit(String xaId) throws Exception {
                httpTransferService.confirm(xaId);
            }

            @Override
            public void rollback(String xaId) throws Exception {
                httpTransferService.cancel(xaId); // TODO add example when service auto-rollbacks by scheduler
            }
        };

        TransactionConfigurator.setup("xa/transferService", callback);
        // After 30 seconds TransactionManager will not recover failed transactions. This parameter depends on external system configuration.
        // We assume that external system automatically rollbacks transaction after >=30 seconds

        // We are using file storage for transaction recovery records. That may be any different storage (even JMS queues)
        MicroserviceXAResource.configure(30000L, new File("C:\\Work\\JAVA tech talks\\Distributed transactions\\java\\distributed-transactions\\banking\\transaction_log\\ws_xa_resource"));
        return callback;
    }

    @Bean("xaPartnerTransferService")
    public SQLTransferService xaPartnerTransferService() {
        return new SQLTransferService(atomikosLocalJdbcTemplate(), atomikosPartnerJdbcTemplate());
    }

    @Bean("xaJmsTemplate")
    public JmsTemplate xaJmsTemplate() {
        JmsTemplate template = new JmsTemplate(amqAtomikosXA());
        template.setSessionTransacted(true); // TODO try this for non-xa template
        return template;
    }

    // TODO create JMS listener container under control of atomikos

    @Bean("xaJdbcTemplate")
    public JdbcTemplate atomikosLocalJdbcTemplate() {
        return new JdbcTemplate(localAtomikosXA());
    }

    @Bean("partnerXaJdbcTemplate")
    public JdbcTemplate atomikosPartnerJdbcTemplate() {
        return new JdbcTemplate(partnerAtomikosXA());
    }

    @Bean
    public ActiveMQXAConnectionFactory amqXA() {
        return new ActiveMQXAConnectionFactory("tcp://localhost:61616");
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosConnectionFactoryBean amqAtomikosXA() {
        AtomikosConnectionFactoryBean ds = new AtomikosConnectionFactoryBean();
        ds.setUniqueResourceName("activemq");
        ds.setMaxPoolSize(10);
        ds.setMinPoolSize(5);
        ds.setXaConnectionFactory(amqXA());
        return ds;
    }

    @Bean("localXADaraSource")
    public XADataSource localXA() {
        MysqlXADataSource xaDataSource = new MysqlXADataSource();
        xaDataSource.setPort(3306);
        xaDataSource.setServerName("localhost");
        xaDataSource.setUser("root");
        xaDataSource.setPassword("root");
        xaDataSource.setDatabaseName("bank1");
        xaDataSource.setPinGlobalTxToPhysicalConnection(true); // https://www.atomikos.com/Documentation/KnownProblems#MySQL_XA_bug
        return xaDataSource;
    }

    @Bean("partnerXA")
    public XADataSource partnerXA() {
        MysqlXADataSource xaDataSource = new MysqlXADataSource();
        xaDataSource.setPort(3306);
        xaDataSource.setServerName("localhost");
        xaDataSource.setUser("root");
        xaDataSource.setPassword("root");
        xaDataSource.setDatabaseName("partner_bank");
        xaDataSource.setPinGlobalTxToPhysicalConnection(true); // https://www.atomikos.com/Documentation/KnownProblems#MySQL_XA_bug
        return xaDataSource;
    }

    @Bean(name = "localAtomikosXA", initMethod = "init", destroyMethod = "close")
    public AtomikosDataSourceBean localAtomikosXA() {
        AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
//        ds.setLogWriter(null); // TODO try custom log writer, do we need log reader?
        ds.setUniqueResourceName("localXA");
        ds.setXaDataSource(localXA());
        ds.setMaxPoolSize(10);
        ds.setMinPoolSize(5);
        ds.setTestQuery("select 1");
        return ds;
    }

    @Bean(name = "partnerAtomikosXA", initMethod = "init", destroyMethod = "close")
    public AtomikosDataSourceBean partnerAtomikosXA() {
        AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
//        ds.setLogWriter(null); // TODO try custom log writer, do we need log reader?
        ds.setUniqueResourceName("partnerXA");
        ds.setXaDataSource(partnerXA());
        ds.setMaxPoolSize(10);
        ds.setMinPoolSize(5);
        ds.setTestQuery("select 1");
        return ds;
    }

    @Primary
    @Bean(name = "xaTransactionManager", initMethod = "init", destroyMethod = "close")
    public TransactionManager xaTransactionManager() {
        UserTransactionManager txManager = new UserTransactionManager();
        txManager.setForceShutdown(false);
        return txManager;
    }

    // We must always get new transaction from the spring application context
    // TODO should we autowire TransactionManager and call getTransaction() instead?
    @Bean
    @Scope("prototype")
    public UserTransactionImp transaction() throws SystemException {
        UserTransactionImp tx = new UserTransactionImp();
        tx.setTransactionTimeout(300);
        return tx;
    }

    // TODO why do we need both transactionService and transactionManager?
    // TODO what is the difference between them?
    @Bean(name = "xaTransactionService", initMethod = "init", destroyMethod = "shutdownWait")
    public UserTransactionServiceImp xaTransactionService() {
        return new UserTransactionServiceImp();
    }

    // TODO add LocalLogAdministrator
}