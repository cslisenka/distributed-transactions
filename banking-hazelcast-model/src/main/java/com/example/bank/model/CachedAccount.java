package com.example.bank.model;

import java.io.Serializable;

public class CachedAccount implements Serializable {

    private String identifier;
    private int balance;

    public CachedAccount(String identifier, int balance) {
        this.identifier = identifier;
        this.balance = balance;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }
}