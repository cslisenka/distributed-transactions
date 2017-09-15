package com.slisenko.examples.xa.mysql.util;

import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlXid;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.SQLException;

public class MyTransactionManager {

    public static void perform(WorkUnit unit) throws SQLException, XAException {
        XADataSource ds1 = createDS("distributed-tx-1");
        XADataSource ds2 = createDS("distributed-tx-2");

        XAConnection xaCon1 = ds1.getXAConnection();
        XAConnection xaCon2 = ds2.getXAConnection();

        // Prepare global transaction identifiers
        Xid xId1 = createXid(1); // pass globally unique ID
        Xid xId2 = createXid(2); // pass globally unique ID

        XAResource xaRes1 = xaCon1.getXAResource();
        XAResource xaRes2 = xaCon2.getXAResource();

        // Indicate work is started
        xaRes1.start(xId1, XAResource.TMNOFLAGS);
        xaRes2.start(xId2, XAResource.TMNOFLAGS);

        // Work with both databases in regular JDBC manner
        Connection con1 = xaCon1.getConnection();
        Connection con2 = xaCon2.getConnection();

        unit.doInTransaction(con1, con2);

        // Indicate that work is ended
        xaRes1.end(xId1, XAResource.TMSUCCESS); // If we want to fail, set TMFAIL
        xaRes2.end(xId2, XAResource.TMSUCCESS); // If we want to fail, set TMFAIL

        // Execute nonxa transactions on both databases
        // Both data sources return vote if commit was OK
        int vote1 = xaRes1.prepare(xId1);
        int vote2 = xaRes2.prepare(xId2);

        log(vote1, vote2);

        if (vote1 == XAResource.XA_OK && vote2 == XAResource.XA_OK) {
            // Commit if all cohorts vote "yes"
            log("Commit global transaction");
            xaRes1.commit(xId1, false);
            xaRes2.commit(xId2, false);
        } else {
            // Rollback if any cohort say no
            log("Rollback global transaction");
            xaRes1.rollback(xId1);
            xaRes2.rollback(xId2);
        }

        con1.close();
        con2.close();

        xaCon1.close();
        xaCon2.close();
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

    public static void log(int vote1, int vote2) {
        log("DB1 " + (XAResource.XA_OK == vote1 ? "OK" : ("FAIL (" + vote1 + ")")));
        log("DB2 " + (XAResource.XA_OK == vote2 ? "OK" : ("FAIL (" + vote2 + ")")));
    }
}