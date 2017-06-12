package com.slisenko.examples.test.event;

import com.slisenko.examples.test.domain.Port;
import com.slisenko.examples.test.domain.Ship;

import java.util.Date;

/**
 * Created by Kanstantsin on 11.06.2017.
 */
public class ArrivalEvent extends DomainEvent {

    private final Ship ship;
    private final Port port;

    public ArrivalEvent(Date date, Ship ship, Port port) {
        super(date);
        this.ship = ship;
        this.port = port;
    }

    @Override
    public void process() {
        // TODO
    }

    public Ship getShip() {
        return ship;
    }

    public Port getPort() {
        return port;
    }
}
