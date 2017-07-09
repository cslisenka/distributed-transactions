package com.example.axonexample.api;

import com.example.axonexample.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;

@RestController
public class MoneyTransferAPI {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MoneyTransferRepository moneyTransferRepository;

    @GetMapping
    public List<Account> getAccounts() {
        return accountRepository.findAll();
    }

    @GetMapping("/{accountIdentifier}/transfers")
    public List<MoneyTransfer> getMoneyTransfers(@PathVariable String accountIdentifier) {
        // TODO find by account identifier
        return moneyTransferRepository.findAll();
    }

    @PostMapping("/transferMoney")
    public MoneyTransfer transferMoney(@RequestBody Map<String, String> request) throws OverdraftException {
        return doTransfer(request.get("from"), request.get("to"), Integer.parseInt(request.get("amount")));
    }

    @Transactional
    protected MoneyTransfer doTransfer(String from, String to, int amount) throws OverdraftException {
        Account accFrom = accountRepository.findOne(from);
        Account accTo = accountRepository.findOne(to);

        // TODO add locks (pessimistic or optimistic)
        accFrom.withdraw(amount);
        accTo.deposit(amount);

        return moneyTransferRepository.save(new MoneyTransfer(accFrom.getIdentifier(), accTo.getIdentifier(), amount));
    }
}