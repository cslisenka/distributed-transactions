package com.example.bank.service;

import com.example.bank.model.AccountDTO;
import com.example.bank.model.CacheUpdateDTO;
import com.example.bank.model.MoneyTransferDTO;
import com.example.bank.model.OverdraftException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.jms.Queue;
import java.util.Date;

@Service
public class LocalService extends AbstractTransferService {

    @Autowired
    @Qualifier("local")
    private JdbcTemplate local;

    @Autowired
    @Qualifier("xaJmsTemplate")
    private JmsTemplate jms;

    @Autowired
    @Qualifier("cacheUpdateQueue")
    private Queue cacheUpdateQueue;

    @Override
    public void doTransfer(MoneyTransferDTO request) throws OverdraftException {
        AccountDTO accountFrom = local.queryForObject("select * from account where identifier = ? for update",
                new AccountDTO.AccountRowMapper(), request.getFrom());
        AccountDTO accountTo = local.queryForObject("select * from account where identifier = ? for update",
                new AccountDTO.AccountRowMapper(), request.getTo());

        if (accountFrom.getBalance() < request.getAmount()) {
            throw new OverdraftException(request.getFrom());
        }

        local.update("update account set balance = ? where identifier = ?",
                accountFrom.getBalance() - request.getAmount(), request.getFrom());
        local.update("update account set balance = ? where identifier = ?",
                accountTo.getBalance() + request.getAmount(), request.getFrom());

        // We need transfer ID to avoid executing money transfer multiple times in case of retries
        Date time = new Date();
        local.update("insert into money_transfers (transfer_id, account_from, account_to, bank_to, amount, date_time) " +
                        "values (?, ?, ?, ?, ?, ?)", request.getTransferId(), request.getFrom(), request.getTo(), "local", request.getAmount(), time);

        // Sending cache update event
        accountFrom.setBalance(accountFrom.getBalance() - request.getAmount());
        request.setDateTime(time);
        jms.send(cacheUpdateQueue, session -> session.createObjectMessage(new CacheUpdateDTO(accountFrom, request)));
    }
}