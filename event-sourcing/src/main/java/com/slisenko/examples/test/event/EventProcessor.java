package com.slisenko.examples.test.event;

import java.util.ArrayList;
import java.util.List;

public class EventProcessor {

    private final List<DomainEvent> log = new ArrayList<>();

    public void process(DomainEvent event) {
        event.process();
        log.add(event);
    }
}