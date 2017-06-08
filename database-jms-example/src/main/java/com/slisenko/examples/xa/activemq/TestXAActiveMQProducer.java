package com.slisenko.examples.xa.activemq;

import com.mysql.jdbc.jdbc2.optional.MysqlXid;
import org.apache.activemq.ActiveMQXAConnectionFactory;

import javax.jms.*;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * https://github.com/apache/activemq-artemis/tree/master/examples/features/standard/xa-send/src/main/java/org/apache/activemq/artemis/jms/example
 */
public class TestXAActiveMQProducer {

    public static void main(String[] args) throws JMSException, XAException {
        XAConnectionFactory xaFactory = new ActiveMQXAConnectionFactory("tcp://localhost:61616");
        XAConnection xaCon = xaFactory.createXAConnection();
        xaCon.start();

        Xid xId = createXid(1); // Global transaction ID
        XASession xaSession = xaCon.createXASession();
        // XAResource is not in sql/jms package, it is in javax.transaction because it is common for all resource types
        XAResource xaResource = xaSession.getXAResource();

        // Mark work as started
        xaResource.start(xId, XAResource.TMNOFLAGS);

        // Work like with regular JMS core
        Session session = xaSession.getSession();
        Destination queue = session.createQueue("JMS.XA.QUEUE1");
        Destination queue2 = session.createQueue("JMS.XA.QUEUE2");

        MessageProducer producer1 = session.createProducer(queue);
        producer1.send(session.createTextMessage("message"));

        MessageProducer producer2 = session.createProducer(queue2);
        producer2.send(session.createTextMessage("message2"));

        xaResource.end(xId, XAResource.TMSUCCESS);

        int vote = xaResource.prepare(xId);

        xaResource.rollback(xId);

        if (vote == XAResource.XA_OK) {
            System.out.println("Commit XA transaction");
            xaResource.commit(xId, false); // Message goes to both queues
        } else {
            System.out.println("Rollback XA transaction");
            xaResource.rollback(xId); // Message doens't go to any queue
        }

        session.close();

        xaSession.close();
        xaCon.close();
    }

    public static Xid createXid(int bids) {
        byte[] gid = new byte[1];
        gid[0] = (byte) 9;
        byte[] bid = new byte[1];
        bid[0] = (byte) bids;
        byte[] gtrid = new byte[64];
        byte[] bqual = new byte[64];
        System.arraycopy(gid, 0, gtrid, 0, 1);
        System.arraycopy(bid, 0, bqual, 0, 1);
        // TODO can we use any Xid implementation when we do transaction across MySQL and ActiveMQ?
        return new MysqlXid(gtrid, bqual, 0x1234);
    }
}