package com.example.bank.model.mapper;

import com.example.bank.model.CachedAccount;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountRowMapper implements RowMapper<CachedAccount> {

    @Override
    public CachedAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CachedAccount(
                rs.getString("identifier"),
                rs.getInt("balance"));
    }
}
