package com.example.bank.partner.model;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class PartnerMoneyTransfer {

    private String transferId;
    private String account;
    private String to;
    private int amount;
    private Status status;
    private String cancellationReason;
    private Date dateTime;

    public PartnerMoneyTransfer(String transferId, String account, String externalAccount,
                                int amount, Status status, String cancellationReason, Date dateTime) {
        this.transferId = transferId;
        this.account = account;
        this.to = externalAccount;
        this.amount = amount;
        this.status = status;
        this.cancellationReason = cancellationReason;
        this.dateTime = dateTime;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getAccount() {
        return account;
    }

    public String getTo() {
        return to;
    }

    public int getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public enum Status {
        RESERVED, CONFIRMED, CANCELLED
    }

    public static class PartnerMoneyTransferRowMapper implements RowMapper<PartnerMoneyTransfer> {

        @Override
        public PartnerMoneyTransfer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PartnerMoneyTransfer(
                    rs.getString("transfer_id"),
                    rs.getString("account"),
                    rs.getString("external_account"),
                    rs.getInt("amount"),
                    PartnerMoneyTransfer.Status.valueOf(rs.getString("status")),
                    rs.getString("cancellation_reason"),
                    rs.getDate("date_time"));
        }
    }
}