package com.example.bank.model;

public class PartnerMoneyTransfer {

    private Integer transferId;

    private String account;
    private String partnerAccount;
    private int amount;
    private Direction direction;

    public PartnerMoneyTransfer(Integer transferId, String account, String partnerAccount, int amount, Direction direction) {
        this.transferId = transferId;
        this.account = account;
        this.partnerAccount = partnerAccount;
        this.amount = amount;
        this.direction = direction;
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