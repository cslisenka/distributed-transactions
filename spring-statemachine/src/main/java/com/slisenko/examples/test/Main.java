package com.slisenko.examples.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.statemachine.StateMachine;

@SpringBootApplication
public class Main implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Autowired
    private StateMachine<OrderState, OrderEvent> order;

    @Override
    public void run(String... strings) throws Exception {
        System.out.println("statemachine");

        System.out.println(order.getState());
        order.sendEvent(OrderEvent.PAY);
        System.out.println(order.getState());
        order.sendEvent(OrderEvent.COMPLETE);
        System.out.println(order.getState());
    }
}