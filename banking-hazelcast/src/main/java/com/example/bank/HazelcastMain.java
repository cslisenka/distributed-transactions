package com.example.bank;

import com.example.bank.model.CachedAccount;
import com.example.bank.model.CachedMoneyTransfer;
import com.example.bank.model.mapper.AccountRowMapper;
import com.example.bank.model.mapper.MoneyTransferRowMapper;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Configuration
@SpringBootApplication
public class HazelcastMain {

    public static final String HAZELCAST_ACCOUNTS = "account";
    public static final String HAZELCAST_TRANSFERS = "transfer";

    public static void main(String[] args) {
        SpringApplication.run(HazelcastMain.class, args);
    }

    @Bean
    public HazelcastInstance hazelcastServer() {
        Config config = new Config();
        config.getNetworkConfig()
                .setPort(5701)
                .setPortAutoIncrement(false); // Should be true in case of cluster
//            .setPortCount(20); // Ports for cluster members

        config.getNetworkConfig().getJoin().getMulticastConfig()
                .setEnabled(false);
        // TODO configure management center application

        HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(config);
        loadDataToCache(hazelcast, jdbcTemplate());

        return hazelcast;
    }

    private void loadDataToCache(HazelcastInstance cache, JdbcTemplate jdbc) {
        List<CachedAccount> accounts = jdbc.query("SELECT * FROM account", new AccountRowMapper());
        Map<String, CachedAccount> accountsMap = cache.getMap(HAZELCAST_ACCOUNTS);
        for (CachedAccount account : accounts) {
            accountsMap.put(account.getIdentifier(), account);
        }

        System.out.println("Uploaded " + accounts.size() + " account s to cache");

        List<CachedMoneyTransfer> transfers = jdbc.query("select * from partner_money_transfer", new MoneyTransferRowMapper());
        for (CachedMoneyTransfer transfer : transfers) {
            cache.getMap(HAZELCAST_TRANSFERS + "_" + transfer.getAccount())
                    .put(transfer.getTransferId(), transfer);
        }

        System.out.println("Uploaded " + transfers.size() + " transfers");
    }

    @Primary
    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(localDataSource());
    }

    @Primary
    @Bean
    public DataSource localDataSource() {
        MysqlDataSource db = new MysqlDataSource();
        db.setPort(3306);
        db.setUser("root");
        db.setPassword("root");
        db.setServerName("localhost");
        db.setDatabaseName("bank1");
        return db;
    }
}