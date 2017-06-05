package com.slisenko.examples.xa;

import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlXid;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TestMySQLTwoDatabases {

    public static void main(String[] args) throws SQLException, XAException {
        XADataSource ds1 = createDS("distributed-tx-1");
        XADataSource ds2 = createDS("distributed-tx-2");

        XAConnection xaCon1 = ds1.getXAConnection();
        XAConnection xaCon2 = ds2.getXAConnection();

        Connection con1 = xaCon1.getConnection();
        Connection con2 = xaCon2.getConnection();

        XAResource xaRes1 = xaCon1.getXAResource();
        XAResource xaRes2 = xaCon2.getXAResource();

        Xid xId1 = createXid(1);
        Xid xId2 = createXid(2);

        xaRes1.start(xId1, XAResource.TMNOFLAGS);
        xaRes2.start(xId2, XAResource.TMNOFLAGS);

        Statement st1 = con1.createStatement();
        Statement st2 = con2.createStatement();

        // Do updates
        st1.executeUpdate("insert into my_table (value) values ('xa-transaction')");
        st2.executeUpdate("insert into my_table_2 (value) values ('xa-transaction')");

        // END both branches - THIS IS MUST
        xaRes1.end(xId1, XAResource.TMSUCCESS); // or TMFAIL
        xaRes2.end(xId2, XAResource.TMSUCCESS); // or TMFAIL

        // Prepare RMs
        xaRes1.prepare(xId1);
        xaRes2.prepare(xId2);

        xaRes1.commit(xId1, false); // or rollback
        xaRes2.commit(xId2, false); // or rollback

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
}