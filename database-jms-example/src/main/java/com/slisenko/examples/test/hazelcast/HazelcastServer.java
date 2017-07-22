package com.slisenko.examples.test.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;

public class HazelcastServer {

    public static void main(String[] args) {
        Config config = new Config();
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig
            .setPort(5701)
            .setPortAutoIncrement(false); // Should be true in case of cluster
//            .setPortCount(20); // Ports for cluster members

        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig
            .getMulticastConfig().setEnabled(false);

        // TODO configure management center application

        Hazelcast.newHazelcastInstance(config);
    }
}
