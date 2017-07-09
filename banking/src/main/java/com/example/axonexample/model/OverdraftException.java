package com.example.axonexample.model;

public class OverdraftException extends Exception {

    public OverdraftException(String accountIdentofoer) {
        super("Can't withdraw from account " + accountIdentofoer + ", not enough money");
    }
}