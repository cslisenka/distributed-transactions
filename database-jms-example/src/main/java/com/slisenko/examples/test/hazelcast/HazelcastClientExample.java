package com.slisenko.examples.test.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;

import java.util.Map;

public class HazelcastClientExample {

    public static void main(String[] args) {
        ClientConfig config = new ClientConfig();
        config.getNetworkConfig().addAddress("localhost");

        HazelcastInstance client = HazelcastClient.newHazelcastClient(config);

        IdGenerator generator = client.getIdGenerator("id");

        // Inserting and printing map
        Map<String, String> map = client.getMap("test");
        map.put(generator.newId() + "", "value");
        printMap(client.getMap("test"));

        // Doing actions within local transaction
        TransactionOptions options = new TransactionOptions()
                .setTransactionType(TransactionOptions.TransactionType.ONE_PHASE);
        TransactionContext context = client.newTransactionContext(options);
        context.beginTransaction();
        System.out.println("Begin transaction " + context.getTxnId());
        TransactionalMap<String, String> tMap = context.getMap("txMap");
        tMap.put(generator.newId() + "", "committed value");
        System.out.println("Commit transaction " + context.getTxnId());
        context.commitTransaction();

        printMap(client.getMap("txMap"));

        context = client.newTransactionContext(options);
        context.beginTransaction();
        System.out.println("Begin transaction " + context.getTxnId());
        tMap = context.getMap("txMap");
        // TODO try tMap.getForUpdate for REPEATABLE_READ
        tMap.put(generator.newId() + "", "value to rollback");
        System.out.println("Rollback transaction " + context.getTxnId());
        context.rollbackTransaction();

        printMap(client.getMap("txMap"));

        client.shutdown();
    }

    public static void printMap(Map<String, String> txMap) {
        System.out.println("txMap content: ");
        for (String key : txMap.keySet()) {
            System.out.println(key + " " + txMap.get(key));
        }
    }
}
