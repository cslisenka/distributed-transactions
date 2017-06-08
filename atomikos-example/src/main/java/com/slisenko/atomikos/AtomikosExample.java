package com.slisenko.atomikos;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.AtomikosConnectionFactoryBean;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import org.apache.activemq.ActiveMQXAConnectionFactory;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.sql.XADataSource;
import javax.transaction.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class AtomikosExample {

    public static void main(String[] args) throws SystemException, SQLException, JMSException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        // Prepare data sources
        AtomikosDataSourceBean db1 = createMySQLDS("distributed-tx-1");
        AtomikosDataSourceBean db2 = createMySQLDS("distributed-tx-2");
        AtomikosConnectionFactoryBean amq = createActiveMQDS("amq");
        UserTransactionManager txManager = new UserTransactionManager();
        txManager.setForceShutdown(false);

        txManager.init();
        db1.init();
        db2.init();
        amq.init();

        // TODO difference with usage of UserTransaction?
        txManager.begin(); // TODO how does it recognize which data sources are involved? ThreadLocals?

        // Work with resources like no transaction manager exist
        Connection con1 = db1.getConnection();
        Connection con2 = db2.getConnection();
        javax.jms.Connection conAmq = amq.createConnection();
        Session session = conAmq.createSession(true, Session.SESSION_TRANSACTED);
        Destination queue = session.createQueue("ATOMIKOS.TEST");
        MessageProducer producer = session.createProducer(queue);

        // Work like with regular JDBC/JMS
        Statement st1 = con1.createStatement();
        Statement st2 = con2.createStatement();

        // Do updates
        st1.executeUpdate("insert into my_table (value) values ('xa-transaction + atomikos')");
        st2.executeUpdate("insert into my_table_2 (value) values ('xa-transaction + atomikos')");
        producer.send(session.createTextMessage("xa-transaction + atomikos")); // TODO use embedded broker and stop it here

//        txManager.commit();
        txManager.rollback();

        session.close();
        conAmq.close();
        con1.close();
        con2.close();

        amq.close();
        db2.close();
        db1.close();
        txManager.close();
    }

    public static AtomikosConnectionFactoryBean createActiveMQDS(String name) {
        AtomikosConnectionFactoryBean ds = new AtomikosConnectionFactoryBean();
        ds.setUniqueResourceName(name);
        ds.setMaxPoolSize(10);
        ds.setMinPoolSize(5);
        ds.setXaConnectionFactory(new ActiveMQXAConnectionFactory("tcp://localhost:61616"));
        return ds;
    }

    public static AtomikosDataSourceBean createMySQLDS(String dbName) {
        AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
        ds.setUniqueResourceName(dbName);
        ds.setXaDataSource(createDS(dbName));
        ds.setMaxPoolSize(10);
        ds.setMinPoolSize(5);
        ds.setTestQuery("select 1");
        return ds;
    }

    public static XADataSource createDS(String dbName) {
        MysqlXADataSource xaDataSource = new MysqlXADataSource();
        xaDataSource.setPort(3306);
        xaDataSource.setServerName("localhost");
        xaDataSource.setUser("root");
        xaDataSource.setPassword("root");
        xaDataSource.setDatabaseName(dbName);
        return xaDataSource;
    }
}
