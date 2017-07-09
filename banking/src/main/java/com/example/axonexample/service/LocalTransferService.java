package com.example.axonexample.service;

import com.example.axonexample.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * Service for transferring money between accounts owned by our bank (within the same DB)
 */
@Service
public class LocalTransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MoneyTransferRepository moneyTransferRepository;

    @Transactional
    public MoneyTransfer doTransfer(String from, String to, int amount) throws OverdraftException {
        Account accFrom = accountRepository.findOne(from);
        Account accTo = accountRepository.findOne(to);

        // TODO add locks (pessimistic or optimistic)
        accFrom.withdraw(amount);
        accTo.deposit(amount);

        return moneyTransferRepository.save(new MoneyTransfer(accFrom.getIdentifier(), accTo.getIdentifier(), amount));
    }
}