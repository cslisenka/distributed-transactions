package com.example.bank.model.mapper;

import com.example.bank.model.PartnerMoneyTransfer;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PartnerMoneyTransferRowMapper implements RowMapper<PartnerMoneyTransfer> {

    @Override
    public PartnerMoneyTransfer mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PartnerMoneyTransfer(
                rs.getInt("transfer_id"),
                rs.getString("account"),
                rs.getString("partner_account"),
                rs.getInt("amount"),
                PartnerMoneyTransfer.Direction.valueOf(rs.getString("direction")));
    }
}