package com.example.bank.model;

public class MoneyTransfer {

    private Integer transferId;

    private String fromAccount;

    private String toAccount;

    private int amount;

    public MoneyTransfer(Integer transferId, String fromAccount, String toAccount, int amount) {
        this.transferId = transferId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
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
