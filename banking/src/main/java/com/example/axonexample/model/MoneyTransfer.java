package com.example.axonexample.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class MoneyTransfer {

    @Id
    @GeneratedValue
    private Integer transferId;

    private String fromAccount;

    private String toAccount;

    private int amount;

    public MoneyTransfer(String fromAccount, String toAccount, int amount) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
    }

    public MoneyTransfer() {
    }

    public Integer getTransferId() {
        return transferId;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public int getAmount() {
        return amount;
    }
}
