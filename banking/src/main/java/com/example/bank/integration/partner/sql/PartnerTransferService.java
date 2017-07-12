package com.example.bank.integration.partner.sql;

import com.example.bank.model.Account;
import com.example.bank.model.OverdraftException;
import com.example.bank.model.mapper.AccountRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Service for transferring money between our bank and partners
 * whose database is available for us, or they provide WS-AT web-services
 */
public class PartnerTransferService {

    private JdbcTemplate local;
    private JdbcTemplate partner;

    public PartnerTransferService(JdbcTemplate local, JdbcTemplate partner) {
        this.local = local;
        this.partner = partner;
    }

    public void doTransfer(String transferId, String localAccountId, String partnerAccountId, int amount) throws OverdraftException {
        // Lock both accounts
        Account localAccount = local.queryForObject("select * from account where identifier = ? for update",
                new AccountRowMapper(), localAccountId);

        if (localAccount.getBalance() < amount) {
            throw new OverdraftException(localAccountId);
        }

        // Change balance local account + save transfer in local DB
        local.update("UPDATE account SET balance = ? WHERE identifier = ?", (localAccount.getBalance() - amount), localAccountId);
        local.update("INSERT INTO partner_money_transfer (transfer_id, account, partner_account, amount, direction) " +
                "VALUES (?, ?, ?, ?, ?)", transferId, localAccountId, partnerAccountId, amount, "OUT");

        // Query for locking
        Account partnerAccount = partner.queryForObject("select * from london_account where identifier = ? for update",
                new AccountRowMapper(), partnerAccountId);

        // Change balance partner account + save transfer in partner DB
        partner.update("UPDATE london_account SET balance = ? WHERE identifier = ?", (partnerAccount.getBalance() + amount), partnerAccountId);
        partner.update("INSERT INTO london_partner_money_transfer (transfer_id, account, partner_account, amount, direction) " +
                "VALUES (?, ?, ?, ?, ?)", transferId, partnerAccountId, localAccountId, amount, "IN");
    }
}