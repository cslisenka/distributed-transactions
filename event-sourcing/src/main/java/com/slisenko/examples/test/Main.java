package com.slisenko.examples.test;

import com.slisenko.examples.test.domain.Cargo;
import com.slisenko.examples.test.domain.Port;
import com.slisenko.examples.test.domain.Ship;
import com.slisenko.examples.test.event.*;

import java.util.Date;

public class Main {

    public static void main(String[] args) {
        EventProcessor processor = new EventProcessor();

        Cargo c = new Cargo("product");
        Ship ship = new Ship("ship");
        Port sfo = new Port("San Francisco", "US");
        Port la = new Port("Los Angeles", "US");
        Port yyv = new Port("Vancouver", "Canada");

        processor.process(new LoadEvent(new Date(), c, ship));
        processor.process(new ArrivalEvent(new Date(), ship, sfo));
        processor.process(new DepartureEvent(new Date(), ship, sfo));
        processor.process(new ArrivalEvent(new Date(), ship, la));
        processor.process(new UnLoadEvent(new Date(), c, ship));
    }
}
