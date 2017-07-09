package com.example.bank.api;

import com.example.bank.commands.ComplaintFiledEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// All event handlers within this class belong to "statistics" group
// Then we can define that "statistics" should get events from the queue
// instead of event bus by default
@ProcessingGroup("statistics")
@RestController
public class StatisticsAPI {

    private ConcurrentHashMap<String, AtomicInteger> statistics = new ConcurrentHashMap<>();

    // The event should be delivered by RabbitMQ
    @EventHandler
    public void on(ComplaintFiledEvent event) {
        System.out.println("Received event " + event);
        statistics.computeIfAbsent(event.getCompany(), k -> new AtomicInteger()).incrementAndGet();
    }

    @GetMapping
    public ConcurrentHashMap<String, AtomicInteger> get() {
        return statistics;
    }
}