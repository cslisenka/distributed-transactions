package com.example.axonexample.service;

import com.example.axonexample.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * Service for transferring money between our bank and partners
 * whose database is available for us, or they provide WS-AT web-services
 */
@Service
public class PartnerTransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PartnerMoneyTransferRepository partnerMoneyTransferRepository;

    // We are only sending money to partners, we don't care about receiving it from them
    // TODO then partners decided to wrap DB in Web-service and hide queue behind it
    @Transactional
    public PartnerMoneyTransfer doTransfer(String accountIdentifier, String partnerAccount, int amount) throws OverdraftException {
        // In our database
        Account account = accountRepository.findOne(accountIdentifier);
        account.withdraw(amount);
        PartnerMoneyTransfer transfer = partnerMoneyTransferRepository.save(new PartnerMoneyTransfer(accountIdentifier, partnerAccount, amount, PartnerMoneyTransfer.Direction.OUT));


        // In partners database
        // TODO

        return transfer;
    }
}