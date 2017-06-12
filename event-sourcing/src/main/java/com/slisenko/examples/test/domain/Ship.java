package com.slisenko.examples.test.domain;

public class Ship {

    private final String name;
    private Port currentPort;

    public Ship(String name) {
        this.name = name;
    }

    public void depart(Port port) {
        currentPort = port;
    }

    public void arrive(Port port) {
        currentPort = port;
    }

    public String getName() {
        return name;
    }

    public Port getCurrentPort() {
        return currentPort;
    }
}