package com.example.axonexample.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Account {

    @Id
    private String identifier;

    private int balance;

    public Account(String identifier, int balance) {
        this.identifier = identifier;
        this.balance = balance;
    }

    public Account() {
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getBalance() {
        return balance;
    }

    public void withdraw(int amount) throws OverdraftException {
        if (this.balance < amount) {
            throw new OverdraftException(identifier);
        }
        this.balance -= amount;
    }

    public void deposit(int amount) {
        this.balance += amount;
    }
}