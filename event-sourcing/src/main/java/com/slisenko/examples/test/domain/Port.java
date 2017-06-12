package com.slisenko.examples.test.domain;

public class Port {
    private final String name;
    private final String country;

    public Port(String name, String country) {
        this.name = name;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }
}
