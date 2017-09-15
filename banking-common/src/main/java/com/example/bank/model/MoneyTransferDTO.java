package com.example.bank.model;

import org.springframework.jdbc.core.RowMapper;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class MoneyTransferDTO implements Serializable {

    public static final String TRANSFER_ID = "transfer_id";
    public static final String ACCOUNT_FROM = "account_from";
    public static final String ACCOUNT_TO = "account_to";
    public static final String BANK_TO = "bank_to";
    public static final String AMOUNT = "amount";
    public static final String DATE_TIME = "date_time";

    private String transferId;
    private String from;
    private String to;
    private String toBank;
    private int amount;
    private Date dateTime;

    public MoneyTransferDTO(String transferId, String from, String to, String bankTo, int amount, Date dateTime) {
        this.transferId = transferId;
        this.from = from;
        this.to = to;
        this.toBank = bankTo;
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
                    rs.getString(TRANSFER_ID),
                    rs.getString(ACCOUNT_FROM),
                    rs.getString(ACCOUNT_TO),
                    rs.getString(BANK_TO),
                    rs.getInt(AMOUNT),
                    rs.getDate(DATE_TIME));
        }
    }

    public MapMessage to(MapMessage msg) throws JMSException {
        msg.setString(TRANSFER_ID, transferId);
        msg.setString(ACCOUNT_FROM, from);
        msg.setString(ACCOUNT_TO, to);
        msg.setString(BANK_TO, toBank);
        msg.setInt(AMOUNT, amount);
        msg.setLong(DATE_TIME, dateTime.getTime());
        return msg;
    }

    public static MoneyTransferDTO from(MapMessage msg) throws JMSException {
        MoneyTransferDTO result = new MoneyTransferDTO();
        result.setTransferId(msg.getString(TRANSFER_ID));
        result.setFrom(msg.getString(ACCOUNT_FROM));
        result.setTo(msg.getString(ACCOUNT_TO));
        result.setToBank(msg.getString(BANK_TO));
        result.setAmount(msg.getInt(AMOUNT));
        result.setDateTime(new Date(msg.getLong(DATE_TIME)));
        return result;
    }

    @Override
    public String toString() {
        return "MoneyTransferDTO{" +
                "transferId='" + transferId + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", toBank='" + toBank + '\'' +
                ", amount=" + amount +
                ", dateTime=" + dateTime +
                '}';
    }
}