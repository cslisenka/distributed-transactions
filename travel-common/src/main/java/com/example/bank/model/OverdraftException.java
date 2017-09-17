package com.example.bank.model;

public class OverdraftException extends Exception {

    public OverdraftException(String account) {
        super("Overdraft for account " + account);
    }
}
