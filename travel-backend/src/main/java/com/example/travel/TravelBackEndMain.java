package com.example.travel;

import ch.maxant.generic_jca_adapter.CommitRollbackCallback;
import ch.maxant.generic_jca_adapter.MicroserviceXAResource;
import ch.maxant.generic_jca_adapter.TransactionConfigurator;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.extra.MessageDrivenContainer;
import com.example.travel.model.Constants;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
import java.io.IOException;

@Configuration
@SpringBootApplication
public class TravelBackEndMain {

    private static final File TX_LOG_PATH = new File("C:\\Work\\JAVA tech talks\\Distributed transactions\\java\\distributed-transactions\\travel-backend\\transaction_log");
    private static final File TX_LOG_PATH_CAR_SERVICE = new File(TX_LOG_PATH, "logs_car_service");
    private static final File TX_LOG_PATH_FLIGHT_SERVICE = new File(TX_LOG_PATH, "logs_flight_service");

	public static void main(String[] args) throws IOException {
	    // Clean atomikos logs
        FileUtils.cleanDirectory(TX_LOG_PATH);
        TX_LOG_PATH_CAR_SERVICE.mkdirs();
        TX_LOG_PATH_FLIGHT_SERVICE.mkdirs();

		SpringApplication.run(TravelBackEndMain.class, args);
	}

	// TODO create performance tests for XA and similar non-XA actions, use JMeter + Dynatrace
	// TODO calculate overhead of XA

	// JMS configuration
	@Bean("jmsTemplate")
	public JmsTemplate jmsTemplate() {
		JmsTemplate template = new JmsTemplate(amqAtomikosXA());
		template.setSessionTransacted(true); // TODO probably doesn't make sense for Atomimks, only needed if nonxa tx used
		return template;
	}

	@Bean("requestQueue")
	public Queue requestQueue() {
		return new ActiveMQQueue(Constants.QUEUE_XA_BOOKING_REQUEST);
	}

	@Bean("responseQueue")
	public Queue responseQueue() {
		return new ActiveMQQueue(Constants.QUEUE_XA_BOOKING_RESPONSE);
	}

	@Autowired
	@Bean(name = "jmsContainer", initMethod = "start", destroyMethod = "stop")
	public MessageDrivenContainer requestJMSContainer(TravelBackendAPI listener) {
		MessageDrivenContainer container = new MessageDrivenContainer();
		container.setAtomikosConnectionFactoryBean(amqAtomikosXA());
		container.setTransactionTimeout(100);
		container.setDestination(requestQueue());
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
	@Bean("agencyDB")
	public JdbcTemplate agencyDB() {
		return new JdbcTemplate(agencyAtomikosDS());
	}

	@Bean("hotelDB")
	public JdbcTemplate hotelDB() {
		return new JdbcTemplate(hotelAtomikosDS());
	}

	@Primary
	@Bean("agencyDS")
	public XADataSource agencyDS() {
		MysqlXADataSource xaDataSource = new MysqlXADataSource();
		xaDataSource.setPort(3306);
		xaDataSource.setServerName("localhost");
		xaDataSource.setUser("root");
		xaDataSource.setPassword("root");
		xaDataSource.setDatabaseName("travel_agency");
		xaDataSource.setPinGlobalTxToPhysicalConnection(true); // https://www.atomikos.com/Documentation/KnownProblems#MySQL_XA_bug
		return xaDataSource;
	}

	@Bean("hotelDS")
	public XADataSource hotelDS() {
		MysqlXADataSource xaDataSource = new MysqlXADataSource();
		xaDataSource.setPort(3306);
		xaDataSource.setServerName("localhost");
		xaDataSource.setUser("root");
		xaDataSource.setPassword("root");
		xaDataSource.setDatabaseName("hotels");
		xaDataSource.setPinGlobalTxToPhysicalConnection(true); // https://www.atomikos.com/Documentation/KnownProblems#MySQL_XA_bug
		return xaDataSource;
	}

	@Bean(name = "agencyAtomikosDS", initMethod = "init", destroyMethod = "close")
	public AtomikosDataSourceBean agencyAtomikosDS() {
		AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
//        ds.setLogWriter(null); // TODO try custom log writer, do we need log reader?
		ds.setUniqueResourceName("agencyDS");
		ds.setXaDataSource(agencyDS());
		ds.setMaxPoolSize(10);
		ds.setMinPoolSize(5);
		ds.setTestQuery("select 1");
		return ds;
	}

	@Bean(name = "hotelAtomikosDS", initMethod = "init", destroyMethod = "close")
	public AtomikosDataSourceBean hotelAtomikosDS() {
		AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
//        ds.setLogWriter(null); // TODO try custom log writer, do we need log reader?
		ds.setUniqueResourceName("hotelDS");
		ds.setXaDataSource(hotelDS());
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

	@Bean("flightService")
	public HTTPClient flightService() {
		return new HTTPClient("localhost:7001/flight");
	}

	@Bean("carService")
	public HTTPClient carService() {
		return new HTTPClient("localhost:7002/car");
	}

    @Bean
    @Autowired
    public CommitRollbackCallback carRentalServiceTransactionWrapper(
            @Qualifier("carService") HTTPClient carService) {
        CommitRollbackCallback callback = new CommitRollbackCallback() {
            @Override
            public void commit(String xaId) throws Exception {
                carService.confirm(xaId);
            }

            @Override
            public void rollback(String xaId) throws Exception {
                carService.cancel(xaId); // TODO add example when service auto-rollbacks by scheduler
            }
        };

        TransactionConfigurator.setup("xa/carService", callback);
        // After 30 seconds TransactionManager will not recover failed transactions. This parameter depends on external system configuration.
        // We assume that external system automatically rollbacks transaction after >=30 seconds

        // We are using file storage for transaction recovery records. That may be any different storage (even JMS queues)
        MicroserviceXAResource.configure(30000L, TX_LOG_PATH_CAR_SERVICE);
        return callback;
    }

	@Bean
	@Autowired
	public CommitRollbackCallback flightBookingServiceTransactionWrapper(
			@Qualifier("flightService") HTTPClient flightService) {
		CommitRollbackCallback callback = new CommitRollbackCallback() {
			@Override
			public void commit(String xaId) throws Exception {
				flightService.confirm(xaId);
			}

			@Override
			public void rollback(String xaId) throws Exception {
				flightService.cancel(xaId); // TODO add example when flightService auto-rollbacks by scheduler
			}
		};

		TransactionConfigurator.setup("xa/flightService", callback);
		// After 30 seconds TransactionManager will not recover failed transactions. This parameter depends on external system configuration.
		// We assume that external system automatically rollbacks transaction after >=30 seconds

		// We are using file storage for transaction recovery records. That may be any different storage (even JMS queues)
		MicroserviceXAResource.configure(30000L, TX_LOG_PATH_FLIGHT_SERVICE);
		return callback;
	}

	@Bean
	public InitializingBean cleanupDB() {
		return () -> {
			hotelDB().update("DELETE FROM available_rooms");
			for (int i = 0; i < 20; i++) {
				hotelDB().update("INSERT INTO available_rooms (status) VALUES('AVAILABLE')");
			}

			agencyDB().update("DELETE FROM booking_item");
			agencyDB().update("DELETE FROM booking");
		};
	}
}