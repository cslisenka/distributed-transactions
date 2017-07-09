package com.example.axonexample.model;

import javax.persistence.*;

@Entity
public class PartnerMoneyTransfer {

    @Id
    @GeneratedValue
    private Integer transferId;

    private String account;
    private String partnerAccount;
    private int amount;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    public PartnerMoneyTransfer(String account, String partnerAccount, int amount, Direction direction) {
        this.account = account;
        this.partnerAccount = partnerAccount;
        this.amount = amount;
        this.direction = direction;
    }

    public PartnerMoneyTransfer() {
    }

    public Integer getTransferId() {
        return transferId;
    }

    public String getAccount() {
        return account;
    }

    public String getPartnerAccount() {
        return partnerAccount;
    }

    public int getAmount() {
        return amount;
    }

    public Direction getDirection() {
        return direction;
    }

    public enum Direction {
        IN, OUT
    }
}