package com.example.bank;

import com.example.bank.model.AccountDTO;
import com.example.bank.model.Constants;
import com.example.bank.model.MoneyTransferDTO;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CacheLoader {

    @Autowired
    private HazelcastInstance cache;

    @Autowired
    private JdbcTemplate jdbc;

    public void loadDataToCache() {
        List<AccountDTO> accounts = jdbc.query("SELECT * FROM account", new AccountDTO.AccountRowMapper());
        Map<String, AccountDTO> accountsMap = cache.getMap(Constants.HAZELCAST_ACCOUNTS);
        for (AccountDTO account : accounts) {
            accountsMap.put(account.getIdentifier(), account);
        }

        System.out.println("Uploaded " + accounts.size() + " accounts to cache");

        List<MoneyTransferDTO> transfers = jdbc.query("select * from money_transfers", new MoneyTransferDTO.MoneyTransferRowMapper());
        for (MoneyTransferDTO transfer : transfers) {
            cache.getMap(Constants.HAZELCAST_TRANSFERS + "_" + transfer.getFrom())
                    .put(transfer.getTransferId(), transfer);
        }

        System.out.println("Uploaded " + transfers.size() + " transfers");
    }
}
