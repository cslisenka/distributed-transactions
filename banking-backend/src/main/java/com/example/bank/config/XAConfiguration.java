package com.example.bank.config;

import ch.maxant.generic_jca_adapter.CommitRollbackCallback;
import ch.maxant.generic_jca_adapter.MicroserviceXAResource;
import ch.maxant.generic_jca_adapter.TransactionConfigurator;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.extra.MessageDrivenContainer;
import com.example.bank.api.XABackendAPI;
import com.example.bank.model.Constants;
import com.example.bank.service.HTTPService;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jta.atomikos.AtomikosConnectionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Queue;
import javax.sql.XADataSource;
import javax.transaction.SystemException;
import java.io.File;

@Configuration
public class XAConfiguration {

    // JMS configuration
    @Bean("xaJms")
    public JmsTemplate xaJms() {
        JmsTemplate template = new JmsTemplate(amqAtomikosXA());
        template.setSessionTransacted(true); // TODO probably doesn't make sense for Atomimks, only needed if nonxa tx used
        return template;
    }

    @Bean("xaMoneyTransferQueue")
    public Queue xaRequestQueue() {
        return new ActiveMQQueue(Constants.QUEUE_XA_TRANSFER);
    }

    @Bean("xaCacheUpdateQueue")
    public Queue xaCacheUpdateQueue() {
        return new ActiveMQQueue(Constants.QUEUE_XA_CACHE_UPDATE);
    }

    @Autowired
    @Bean(name = "xaJmsContainer", initMethod = "start", destroyMethod = "stop")
    public MessageDrivenContainer requestProcessingContainer(XABackendAPI listener) {
        MessageDrivenContainer container = new MessageDrivenContainer();
        container.setAtomikosConnectionFactoryBean(amqAtomikosXA());
        container.setTransactionTimeout(100);
        container.setDestination(xaRequestQueue());
        container.setMessageListener(listener);
        return container;
    }

    @Bean("xaConnectionFactory")
    public ActiveMQXAConnectionFactory xaConnectionFactory() {
        ActiveMQXAConnectionFactory amq = new ActiveMQXAConnectionFactory("tcp://localhost:61616");

        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(10);
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
    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosConnectionFactoryBean amqAtomikosXA() {
        AtomikosConnectionFactoryBean ds = new AtomikosConnectionFactoryBean();
        ds.setUniqueResourceName("activemq");
        ds.setMaxPoolSize(10);
        ds.setMinPoolSize(5);
        ds.setXaConnectionFactory(xaConnectionFactory());
        return ds;
    }

    // DB configuration
    @Bean("xaLocal")
    public JdbcTemplate xaLocal() {
        return new JdbcTemplate(localAtomikosXA());
    }

    @Bean("xaPartner")
    public JdbcTemplate xaPartner() {
        return new JdbcTemplate(partnerAtomikosXA());
    }

    @Primary
    @Bean("localXADataSource")
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

    @Bean("partnerXADataSource")
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
    public UserTransactionManager xaTransactionManager() throws SystemException {
        UserTransactionManager txManager = new UserTransactionManager();
        txManager.setTransactionTimeout(300);
        txManager.setForceShutdown(false);
        return txManager;
    }

    // TODO why do we need both transactionService and transactionManager?
    // TODO what is the difference between them?
    @Bean(name = "xaTransactionService", initMethod = "init", destroyMethod = "shutdownWait")
    public UserTransactionServiceImp xaTransactionService() {
        return new UserTransactionServiceImp();
    }

    // TODO add LocalLogAdministrator

    @Bean
    @Autowired
    public CommitRollbackCallback partnerMoneyTransfer(HTTPService httpService) {
        CommitRollbackCallback callback = new CommitRollbackCallback() {
            @Override
            public void commit(String xaId) throws Exception {
                httpService.confirm(xaId);
            }

            @Override
            public void rollback(String xaId) throws Exception {
                httpService.cancel(xaId); // TODO add example when service auto-rollbacks by scheduler
            }
        };

        TransactionConfigurator.setup("xa/transferService", callback);
        // After 30 seconds TransactionManager will not recover failed transactions. This parameter depends on external system configuration.
        // We assume that external system automatically rollbacks transaction after >=30 seconds

        // We are using file storage for transaction recovery records. That may be any different storage (even JMS queues)
        MicroserviceXAResource.configure(30000L, new File("C:\\Work\\JAVA tech talks\\Distributed transactions\\java\\distributed-transactions\\banking-backend\\transaction_log\\ws_xa_resource"));
        return callback;
    }
}