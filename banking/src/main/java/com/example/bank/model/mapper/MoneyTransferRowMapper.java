package com.example.bank.model.mapper;

import com.example.bank.model.MoneyTransfer;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MoneyTransferRowMapper implements RowMapper<MoneyTransfer> {

    @Override
    public MoneyTransfer mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MoneyTransfer(
                rs.getInt("transfer_id"),
                rs.getString("from_account"),
                rs.getString("to_account"),
                rs.getInt("amount"));
    }
}