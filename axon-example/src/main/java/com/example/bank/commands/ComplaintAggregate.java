package com.example.bank.commands;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@Aggregate
public class ComplaintAggregate {

    @AggregateIdentifier
    private String identifier;

    // Action performed by user
    @CommandHandler
    public ComplaintAggregate(FileComplaintCommand cmd) {
        // Submit event that aggregate was changed
        apply(new ComplaintFiledEvent(cmd.getId(), cmd.getCompany(), cmd.getDescription()));
    }

    // Construct aggregate based on historic events
    // Commands may cause side-effects (sending emails, etc), so when we reconstructing the object
    // it may be better to do it by events
    @EventSourcingHandler
    public void on(ComplaintFiledEvent event) {
        identifier = event.getIdentifier();
        // TODO set other parameters?
    }
}