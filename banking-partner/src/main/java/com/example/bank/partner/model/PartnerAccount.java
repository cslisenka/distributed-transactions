package com.example.bank.partner.model;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PartnerAccount {

    private String identifier;

    private int balance;

    public PartnerAccount(String identifier, int balance) {
        this.identifier = identifier;
        this.balance = balance;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getBalance() {
        return balance;
    }

    public static class PartnerAccountRowMapper implements RowMapper<PartnerAccount> {

        @Override
        public PartnerAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PartnerAccount(
                    rs.getString("identifier"),
                    rs.getInt("balance"));
        }
    }
}