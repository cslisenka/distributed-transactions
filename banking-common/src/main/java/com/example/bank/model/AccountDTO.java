package com.example.bank.model;

import org.springframework.jdbc.core.RowMapper;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDTO implements Serializable {

    private String identifier;
    private int balance;

    public AccountDTO(String identifier, int balance) {
        this.identifier = identifier;
        this.balance = balance;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public static class AccountRowMapper implements RowMapper<AccountDTO> {
        @Override
        public AccountDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AccountDTO(
                    rs.getString("identifier"),
                    rs.getInt("balance"));
        }
    }
}