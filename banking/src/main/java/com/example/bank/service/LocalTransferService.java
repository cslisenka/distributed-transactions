package com.example.bank.service;

import com.example.bank.model.Account;
import com.example.bank.model.OverdraftException;
import com.example.bank.model.mapper.AccountRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for transferring money between accounts owned by our bank (within the same DB)
 */
@Service
public class LocalTransferService {

    @Autowired
    @Qualifier("localJdbc")
    private JdbcTemplate jdbc;

    @Transactional
    public void doTransfer(String from, String to, int amount) throws OverdraftException {
        Account accountFrom = jdbc.queryForObject("select * from account where identifier = ? for update",
                new AccountRowMapper(), from);
        Account accountTo = jdbc.queryForObject("select * from account where identifier = ? for update",
                new AccountRowMapper(), to);

        if (accountFrom.getBalance() < amount) {
            throw new OverdraftException(from);
        }

        jdbc.update("update account set balance = ? where identifier = ?",
                accountFrom.getBalance() - amount, from);
        jdbc.update("update account set balance = ? where identifier = ?",
                accountTo.getBalance() + amount, to);

        jdbc.update("insert into money_transfer (from_account, to_account, amount) values (?, ?, ?)",
            from, to, amount);
    }
}