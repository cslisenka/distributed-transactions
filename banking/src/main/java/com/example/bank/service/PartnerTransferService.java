package com.example.bank.service;

import com.example.bank.model.OverdraftException;
import com.example.bank.model.PartnerMoneyTransfer;
import org.springframework.stereotype.Service;

/**
 * Service for transferring money between our bank and partners
 * whose database is available for us, or they provide WS-AT web-services
 */
@Service
public class PartnerTransferService {

    // We are only sending money to partners, we don't care about receiving it from them
    // TODO then partners decided to wrap DB in Web-service and hide queue behind it
    // XA transaction needed
//    @Transactional
    public PartnerMoneyTransfer doTransfer(String accountIdentifier, String partnerAccount, int amount) throws OverdraftException {
        // In our database
        PartnerMoneyTransfer transfer = doLocal(accountIdentifier, partnerAccount, amount);

        // In partners database
        // TODO

        return transfer;
    }

//    @Transactional("localTransactionManager")
    protected PartnerMoneyTransfer doLocal(String accountIdentifier, String partnerAccount, int amount) throws OverdraftException {
//        Account account = accountRepository.findOne(accountIdentifier);
//        account.withdraw(amount);
//        return partnerMoneyTransferRepository.save(new PartnerMoneyTransfer(accountIdentifier, partnerAccount, amount, PartnerMoneyTransfer.Direction.OUT));
        return null;
    }
}