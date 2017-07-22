package com.example.bank.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

public class HazelcastBankingCache {

    public static void main(String[] args) {
        Config config = new Config();
        config.getNetworkConfig()
                .setPort(5701)
                .setPortAutoIncrement(false); // Should be true in case of cluster
//            .setPortCount(20); // Ports for cluster members

        config.getNetworkConfig().getJoin().getMulticastConfig()
                .setEnabled(false);
        // TODO configure management center application

        Hazelcast.newHazelcastInstance(config);
    }
}