package com.example.bank;

import ch.maxant.generic_jca_adapter.CommitRollbackCallback;
import ch.maxant.generic_jca_adapter.MicroserviceXAResource;
import ch.maxant.generic_jca_adapter.TransactionConfigurator;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.extra.MessageDrivenContainer;
import com.example.bank.model.Constants;
import com.example.bank.service.AbstractTransferService;
import com.example.bank.service.HTTPService;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jta.atomikos.AtomikosConnectionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.RestTemplate;

import javax.jms.Queue;
import javax.sql.XADataSource;
import javax.transaction.SystemException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
@SpringBootApplication
public class BankBackendMain {

	public static void main(String[] args) {
		SpringApplication.run(BankBackendMain.class, args);
	}

	// TODO create performance tests for XA and similar non-XA actions, use JMeter + Dynatrace
	// TODO calculate overhead of XA
	// TODO add cleanup task (if we passed flag as input parameter)
	// TODO cleanup should restore database into initial state + clear all TransactionManager logs

	@Bean
	public InitializingBean init() {
		return () -> System.out.println("TODO Clean atomikos logs");
	}

	@Bean("moneyTransferQueue")
	public Queue requestQueue() {
		return new ActiveMQQueue(Constants.TRANSFER_QUEUE);
	}

	@Bean("cacheUpdateQueue")
	public Queue cacheUpdateQueue() {
		return new ActiveMQQueue(Constants.CACHE_UPDATE_QUEUE);
	}

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

	@Bean("xaJmsTemplate")
	public JmsTemplate xaJmsTemplate() {
		JmsTemplate template = new JmsTemplate(amqAtomikosXA());
		template.setSessionTransacted(true); // TODO probably doesn't make sense for Atomimks, only needed if local tx used
		return template;
	}

	@Autowired
	@Bean(initMethod = "start", destroyMethod = "stop")
	public MessageDrivenContainer requestProcessingContainer(BackendAPI listener) {
		MessageDrivenContainer container = new MessageDrivenContainer();
		container.setAtomikosConnectionFactoryBean(amqAtomikosXA());
		container.setTransactionTimeout(100);
		container.setDestination(requestQueue());
		container.setMessageListener(listener);
		return container;
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

	@Bean
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
	@Bean("localXA")
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

	@Bean("local")
	public JdbcTemplate local() {
		return new JdbcTemplate(localAtomikosXA());
	}

	@Bean("partner")
	public JdbcTemplate partner() {
		return new JdbcTemplate(partnerAtomikosXA());
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}