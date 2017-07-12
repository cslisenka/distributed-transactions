package com.example.bank.partner.model;

public class Account {

    private String identifier;

    private int balance;

    public Account(String identifier, int balance) {
        this.identifier = identifier;
        this.balance = balance;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getBalance() {
        return balance;
    }
}