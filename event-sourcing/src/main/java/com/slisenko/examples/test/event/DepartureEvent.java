package com.slisenko.examples.test.event;

import com.slisenko.examples.test.domain.Port;
import com.slisenko.examples.test.domain.Ship;

import java.util.Date;

public class DepartureEvent extends DomainEvent {

    private final Ship ship;
    private final Port port;

    public DepartureEvent(Date date, Ship ship, Port port) {
        super(date);
        this.ship = ship;
        this.port = port;
    }

    public Ship getShip() {
        return ship;
    }

    public Port getPort() {
        return port;
    }

    @Override
    void process() {
        ship.depart(port);
    }
}