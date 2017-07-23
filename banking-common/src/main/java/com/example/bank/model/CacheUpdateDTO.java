package com.example.bank.model;

import java.io.Serializable;

public class CacheUpdateDTO implements Serializable {

    private AccountDTO account;
    private MoneyTransferDTO moneyTransfer;

    public CacheUpdateDTO(AccountDTO account, MoneyTransferDTO moneyTransfer) {
        this.account = account;
        this.moneyTransfer = moneyTransfer;
    }

    public AccountDTO getAccount() {
        return account;
    }

    public MoneyTransferDTO getMoneyTransfer() {
        return moneyTransfer;
    }
}