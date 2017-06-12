package com.slisenko.examples.test.event;

import java.util.Date;

public abstract class DomainEvent {

    private Date processed;
    private Date created;

    public DomainEvent(Date processed) {
        this.processed = processed;
        this.created = new Date();
    }

    abstract void process();
}
