package com.example.bank.service;

import com.example.bank.model.*;

import javax.resource.ResourceException;

public abstract class AbstractTransferService {

    public abstract void doTransfer(MoneyTransferDTO request) throws Exception;
}