package com.example.bank.model.mapper;

import com.example.bank.model.CachedMoneyTransfer;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MoneyTransferRowMapper implements RowMapper<CachedMoneyTransfer> {

    @Override
    public CachedMoneyTransfer mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CachedMoneyTransfer(
                rs.getString("transfer_id"),
                rs.getString("account"),
                rs.getString("partner_account"),
                rs.getInt("amount"),
                CachedMoneyTransfer.Direction.valueOf(rs.getString("direction")));
    }
}