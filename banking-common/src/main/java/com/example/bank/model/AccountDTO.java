package com.example.bank.model;

import org.springframework.jdbc.core.RowMapper;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDTO implements Serializable {

    public static final String IDENTIFIER = "identifier";
    public static final String BALANCE = "balance";

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
                    rs.getString(IDENTIFIER),
                    rs.getInt(BALANCE));
        }
    }

    public void to(MapMessage msg) throws JMSException {
        msg.setString(IDENTIFIER, identifier);
        msg.setInt(BALANCE, balance);
    }

    public static AccountDTO from(MapMessage msg) throws JMSException {
        return new AccountDTO(msg.getString(IDENTIFIER), msg.getInt(BALANCE));
    }

    @Override
    public String toString() {
        return "AccountDTO{" +
                "identifier='" + identifier + '\'' +
                ", balance=" + balance +
                '}';
    }
}