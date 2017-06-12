package com.slisenko.examples.test.event;

import com.slisenko.examples.test.domain.Cargo;
import com.slisenko.examples.test.domain.Ship;

import java.util.Date;

public class UnLoadEvent extends DomainEvent {

    private final Cargo cargo;
    private final Ship ship;

    public UnLoadEvent(Date processed, Cargo cargo, Ship ship) {
        super(processed);
        this.cargo = cargo;
        this.ship = ship;
    }

    @Override
    void process() {
        
    }

    public Cargo getCargo() {
        return cargo;
    }

    public Ship getShip() {
        return ship;
    }
}
