package com.example.bank.partner.model.mapper;

import com.example.bank.partner.model.Account;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountRowMapper implements RowMapper<Account> {

    @Override
    public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Account(
                rs.getString("identifier"),
                rs.getInt("balance"));
    }
}
