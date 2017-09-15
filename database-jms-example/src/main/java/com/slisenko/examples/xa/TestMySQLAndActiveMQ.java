package com.slisenko.examples.xa;

import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlXid;
import org.apache.activemq.ActiveMQXAConnectionFactory;

import javax.jms.*;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TestMySQLAndActiveMQ {

    public static void main(String[] args) throws JMSException, SQLException, XAException {
        XADataSource xaMySQLDs = createMySQLDS("distributed-tx-1");
        XADataSource xaMySQLDs2 = createMySQLDS("distributed-tx-2");

        XAConnection xaActiveMQCon = createActiveMQXAConnection();
        xaActiveMQCon.start();
        javax.sql.XAConnection xaCon1 = xaMySQLDs.getXAConnection();
        javax.sql.XAConnection xaCon2 = xaMySQLDs2.getXAConnection();

        // Prepare global transaction identifiers
        Xid xId1 = createXid(1);
        Xid xId2 = createXid(2);
        Xid xId3 = createXid(3);

        XASession xaActiveMQSession = xaActiveMQCon.createXASession();

        XAResource xaMySQLRes1 = xaCon1.getXAResource();
        XAResource xaMySQLRes2 = xaCon2.getXAResource();
        XAResource xaActiveMQRes = xaActiveMQSession.getXAResource();

        // Indicate work is started
        xaMySQLRes1.start(xId1, XAResource.TMNOFLAGS);
        xaMySQLRes2.start(xId2, XAResource.TMNOFLAGS);
        xaActiveMQRes.start(xId3, XAResource.TMNOFLAGS);

        // Work with both databases in regular JDBC manner
        Connection con1 = xaCon1.getConnection();
        Connection con2 = xaCon2.getConnection();
        Session session = xaActiveMQSession.getSession();
        Queue queue = session.createQueue("JMS.XA.WITH.DB");
        MessageProducer producer = session.createProducer(queue);

        // Work like with regular JDBC/JMS
        Statement st1 = con1.createStatement();
        Statement st2 = con2.createStatement();

        // Do updates
        st1.executeUpdate("insert into my_table (value) values ('xa-transaction + JMS')");
        st2.executeUpdate("insert into my_table_2 (value) values ('xa-transaction + JMS')");
        producer.send(session.createTextMessage("xa-transaction + JMS")); // TODO use embedded broker and stop it here

        // TODO check message received/SQL executed

        // Indicate that work is ended
        xaMySQLRes1.end(xId1, XAResource.TMSUCCESS); // If we want to fail, set TMFAIL
        xaMySQLRes2.end(xId2, XAResource.TMSUCCESS); // If we want to fail, set TMFAIL
        xaActiveMQRes.end(xId3, XAResource.TMSUCCESS);

        // Execute nonxa transactions on both databases
        // Both data sources return vote if commit was OK
        int vote1 = xaMySQLRes1.prepare(xId1);
        int vote2 = xaMySQLRes2.prepare(xId2);
        int vote3 = xaActiveMQRes.prepare(xId3);

        log(vote1, vote2, vote3);

        if (vote1 == XAResource.XA_OK && vote2 == XAResource.XA_OK && vote3 == XAResource.XA_OK) {
            // Commit if all cohorts vote "yes"
            log("Commit global transaction");
            xaMySQLRes1.commit(xId1, false);
            xaMySQLRes2.commit(xId2, false);
            xaActiveMQRes.commit(xId3, false);
        } else {
            // Rollback if any cohort say no
            log("Rollback global transaction");
            xaMySQLRes1.rollback(xId1);
            xaMySQLRes2.rollback(xId2);
            xaActiveMQRes.rollback(xId3);
        }

        con1.close();
        con2.close();
        session.close();

        xaCon1.close();
        xaCon2.close();
        xaActiveMQSession.close();
        xaActiveMQCon.close();
    }

    public static XAConnection createActiveMQXAConnection() throws JMSException {
        XAConnectionFactory xaFactory = new ActiveMQXAConnectionFactory("tcp://localhost:61616");
        return xaFactory.createXAConnection();
    }

    public static XADataSource createMySQLDS(String dbName) {
        MysqlXADataSource xaDataSource = new MysqlXADataSource();
        xaDataSource.setPort(3306);
        xaDataSource.setServerName("localhost");
        xaDataSource.setUser("root");
        xaDataSource.setPassword("root");
        xaDataSource.setDatabaseName(dbName);
        return xaDataSource;
    }

    // TODO what is this, transaction id? Can we pass it thru web-service calls? (I saw transactionId property in SOAP headers)
    public static Xid createXid(int bids) {
        byte[] gid = new byte[1];
        gid[0] = (byte) 9;
        byte[] bid = new byte[1];
        bid[0] = (byte) bids;
        byte[] gtrid = new byte[64];
        byte[] bqual = new byte[64];
        System.arraycopy(gid, 0, gtrid, 0, 1);
        System.arraycopy(bid, 0, bqual, 0, 1);
        return new MysqlXid(gtrid, bqual, 0x1234);
    }

    public static void log(String message) {
        System.out.println(message);
    }

    public static void log(int vote1, int vote2, int vote3) {
        log("DB1 " + (XAResource.XA_OK == vote1 ? "OK" : ("FAIL (" + vote1 + ")")));
        log("DB2 " + (XAResource.XA_OK == vote2 ? "OK" : ("FAIL (" + vote2 + ")")));
        log("ActiveMQ " + (XAResource.XA_OK == vote3 ? "OK" : ("FAIL (" + vote3 + ")")));
    }
}