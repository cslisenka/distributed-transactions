package com.example.axonexample.queries;

import com.example.axonexample.commands.ComplaintFiledEvent;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ComplaintQueryModelUpdater {

    @Autowired
    private ComplaintQueryObjectRepository repository;

    // Storing data in query model
    @EventHandler
    public void on(ComplaintFiledEvent event) {
        repository.save(new ComplaintQueryObject(event.getIdentifier(), event.getCompany(), event.getDescription()));
    }
}