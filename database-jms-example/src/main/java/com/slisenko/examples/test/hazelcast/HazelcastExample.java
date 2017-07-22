package com.slisenko.examples.test.hazelcast;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import java.util.Map;

public class HazelcastExample {

    public static void main(String[] args) throws InterruptedException {
        HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance();

        Thread.sleep(1000);

        Map<String, String> map = hazelcast.getMap("test");
        map.put("test", "1");
        map.put("test2", "2");

        System.out.println(hazelcast.getMap("test"));

        hazelcast.shutdown();
    }
}