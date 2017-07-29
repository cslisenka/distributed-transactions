package com.example.bank.partner;

import com.example.bank.partner.model.OverdraftException;
import com.example.bank.partner.model.PartnerAccount;
import com.example.bank.partner.model.PartnerMoneyTransfer;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

// TODO rename to external bank application and make additional database
@RestController
@Configuration
@SpringBootApplication
public class PartnerMain {

    public static final String CONFIRMED = "CONFIRMED";
    public static final String RESERVED = "RESERVED";
    public static final String CANCELLED = "CANCELLED";

	public static void main(String[] args) {
		SpringApplication.run(PartnerMain.class, args);
	}

	@GetMapping
	public List<PartnerAccount> getAccounts() {
		return db().query("select * from partner_account", new PartnerAccount.PartnerAccountRowMapper());
	}

	@GetMapping("/transfer/status/{status}")
	public List<PartnerMoneyTransfer> getMoneyTransfers(@PathVariable String status) {
		if (status != null) {
			return db().query("select * from partner_money_transfer WHERE status = ?", new PartnerMoneyTransfer.PartnerMoneyTransferRowMapper(), status);
		} else {
			return db().query("select * from partner_money_transfer", new PartnerMoneyTransfer.PartnerMoneyTransferRowMapper());
		}
	}

	// TODO add transfer from partners to our bank
	@PostMapping("/transfer/reserve")
	public String reserveMoney(@RequestBody Map<String, String> request) throws OverdraftException {
		String transferId = UUID.randomUUID().toString();

		transaction().execute(tx -> {
            int rows = db().update("INSERT INTO partner_money_transfer (transfer_id, account, external_account, " +
                "amount, status, date_time) VALUES (?, ?, ?, ?, ?, ?)",
                transferId, request.get("to"), request.get("from"),
                Integer.parseInt(request.get("amount")), RESERVED, new Date());

            if (rows != 1) {
                tx.setRollbackOnly();
                return false;
            }

            return true;
        });

		// TODO run background process to cancel all transactions which older then 30 minutes

        return transferId;
	}

    @PostMapping("/transfer/{transferId}/confirm")
    public void confirmTransfer(@PathVariable String transferId) throws OverdraftException {
		transaction().execute(tx -> {
            // Search and lock money transfer
            PartnerMoneyTransfer transfer = db().queryForObject("SELECT * FROM partner_money_transfer WHERE transfer_id = ? AND status = ? FOR UPDATE",
                    new PartnerMoneyTransfer.PartnerMoneyTransferRowMapper(), transferId, RESERVED);

            // Search and lock partnerAccount
            PartnerAccount partnerAccount = db().queryForObject("SELECT * FROM partner_account WHERE identifier=? FOR UPDATE",
                    new PartnerAccount.PartnerAccountRowMapper(), transfer.getAccount());

            // No validation needed for incoming transfer, needed for outgoing transfer
            int transferUpdatedRows = db().update("UPDATE partner_money_transfer SET status = ? WHERE transfer_id = ? AND status = ?",
                    CONFIRMED, transferId, RESERVED);

            int accountUpdatedRows = db().update("UPDATE partner_account SET balance = ? WHERE identifier = ?",
                    partnerAccount.getBalance() + transfer.getAmount(), partnerAccount.getIdentifier());

            boolean isSuccess = (transferUpdatedRows == 1) && (accountUpdatedRows == 1);
            if (!isSuccess) {
                tx.setRollbackOnly();
            }
            return isSuccess;
        });
    }

    @PostMapping("/transfer/{transferId}/cancel")
    public void cancelTransfer(@PathVariable String transferId) throws OverdraftException {
		transaction().execute(tx -> {
            int updatedRows = db().update("UPDATE partner_money_transfer SET status = ?, cancellation_reason = ? " +
                "WHERE transfer_id = ? AND status = ?",
                    CANCELLED, "cancelled by web-service call", transferId, RESERVED);

            boolean isSuccess = (updatedRows == 1);
            if (!isSuccess) {
                tx.setRollbackOnly();
            }
            return isSuccess;
        });
    }

    // Needed by XA resource
    @GetMapping("/transfer/unfinished")
    public List<String> getUnfinishedTransfers() {
		return db().queryForList("select transfer_id from partner_money_transfer WHERE status = ?", String.class, RESERVED);
	}

    @Bean
    public PlatformTransactionManager transactionManager() {
		return new DataSourceTransactionManager(dataSource());
	}

	// For manual management of local transactions
	@Bean
	public TransactionTemplate transaction() {
		return new TransactionTemplate(transactionManager());
	}

	@Bean
	public JdbcTemplate db() {
		return new JdbcTemplate(dataSource());
	}

	// TODO change to Quartz and persistent guaranteed job execution
	@Bean
	public ScheduledExecutorService executor() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//        scheduler.scheduleAtFixedRate()
        return scheduler;
    }

	// TODO add quartz for persistent RESERVED to CALCELLED updates
	// TODO quartz job must clear all RESERVED money if they are not confiemed within
	// TODO add code which creates database schema

	@Bean
	public DataSource dataSource() {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setPort(3306);
		dataSource.setServerName("localhost");
		dataSource.setUser("root");
		dataSource.setPassword("root");
		dataSource.setDatabaseName("partner_bank");
		return dataSource;
	}
}