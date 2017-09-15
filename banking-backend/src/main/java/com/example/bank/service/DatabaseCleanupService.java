package com.example.bank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class DatabaseCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleanupService.class);

    @Autowired
    @Qualifier("local")
    private JdbcTemplate local;

    @Autowired
    @Qualifier("partner")
    private JdbcTemplate partner;

    @PostConstruct
    public void cleanupDB() {
        local.execute("DELETE FROM money_transfer");
        local.execute("DELETE FROM account");

        local.execute("INSERT INTO account (identifier, balance) values ('acc1', 10000)");
        local.execute("INSERT INTO account (identifier, balance) values ('acc2', 20000)");

        partner.execute("DELETE FROM partner_money_transfer");
        partner.execute("DELETE FROM partner_account");

        partner.execute("INSERT INTO partner_account (identifier, balance) values ('p_acc1', 0)");
        partner.execute("INSERT INTO partner_account (identifier, balance) values ('p_acc2', 0)");

        logger.info("Database cleanup completed");
    }
}