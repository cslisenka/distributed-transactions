package com.example.bank.partner.model;

public class PartnerMoneyTransfer {

    private String transferId;
    private String account;
    private String partnerAccount;
    private int amount;
    private Direction direction;
    private Status status;
    private String cancellationReason;

    public PartnerMoneyTransfer(String transferId, String account, String partnerAccount,
                                int amount, Direction direction, Status status, String cancellationReason) {
        this.transferId = transferId;
        this.account = account;
        this.partnerAccount = partnerAccount;
        this.amount = amount;
        this.direction = direction;
        this.status = status;
        this.cancellationReason = cancellationReason;
    }

    public String getTransferId() {
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

    public Status getStatus() {
        return status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public enum Direction {
        IN, OUT
    }

    public enum Status {
        RESERVED, CONFIRMED, CANCELLED
    }
}