package com.example.bank;

import com.example.bank.model.AccountDTO;
import com.example.bank.model.Constants;
import com.example.bank.model.MoneyTransferDTO;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
public class CacheLoader {

    private static final Logger logger = LoggerFactory.getLogger(CacheLoader.class);

    @Autowired
    private HazelcastInstance cache;

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void loadDataToCache() {
        List<AccountDTO> accounts = jdbc.query("select * from account", new AccountDTO.AccountRowMapper());
        Map<String, AccountDTO> accountsMap = cache.getMap(Constants.HAZELCAST_ACCOUNTS);
        for (AccountDTO account : accounts) {
            accountsMap.put(account.getIdentifier(), account);
            logger.info("Loading account {} to cache", account.getIdentifier());
        }

        logger.info("{} accounts loaded to cache", accounts.size());

        List<MoneyTransferDTO> transfers = jdbc.query("select * from money_transfer", new MoneyTransferDTO.MoneyTransferRowMapper());
        for (MoneyTransferDTO transfer : transfers) {
            cache.getMap(Constants.HAZELCAST_TRANSFERS + "_" + transfer.getFrom())
                    .put(transfer.getTransferId(), transfer);
            cache.getMap(Constants.HAZELCAST_TRANSFERS + "_" + transfer.getTo())
                    .put(transfer.getTransferId(), transfer);
            logger.info("Loading transfer {} to cache", transfer.getTransferId());
        }

        logger.info("{} transfers loaded to cache", transfers.size());
    }
}