package com.example.bank.model;

import org.springframework.jdbc.core.RowMapper;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class MoneyTransferDTO implements Serializable { // For Hazelcast serialization

    private String transferId;
    private String from;
    private String to;
    private String toBank;
    private int amount;
    private Date dateTime;

    public MoneyTransferDTO(String transferId, String from, String to, String toBank, int amount, Date dateTime) {
        this.transferId = transferId;
        this.from = from;
        this.to = to;
        this.toBank = toBank;
        this.amount = amount;
        this.dateTime = dateTime;
    }

    public MoneyTransferDTO() {
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setToBank(String toBank) {
        this.toBank = toBank;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public int getAmount() {
        return amount;
    }

    public String getToBank() {
        return toBank;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public static class MoneyTransferRowMapper implements RowMapper<MoneyTransferDTO> {

        @Override
        public MoneyTransferDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MoneyTransferDTO(
                    rs.getString("transfer_id"),
                    rs.getString("account_from"),
                    rs.getString("account_to"),
                    rs.getString("bank_to"),
                    rs.getInt("amount"),
                    rs.getDate("date_time"));
        }
    }

    public void copyTo(MapMessage msg) throws JMSException {
        msg.setString("transfer_id", transferId);
        msg.setString("account_from", from);
        msg.setString("account_to", to);
        msg.setString("bank_to", toBank);
        msg.setInt("amount", amount);
        msg.setLong("date_time", dateTime.getTime());
    }

    public static MoneyTransferDTO createFrom(MapMessage msg) throws JMSException {
        MoneyTransferDTO result = new MoneyTransferDTO();
        result.setTransferId(msg.getString("transfer_id"));
        result.setFrom(msg.getString("account_from"));
        result.setTo(msg.getString("account_to"));
        result.setToBank(msg.getString("bank_to"));
        result.setToBank(msg.getString("bank_to"));
        result.setAmount(msg.getInt("amount"));
        result.setDateTime(new Date(msg.getLong("date_time")));
        return result;
    }
}