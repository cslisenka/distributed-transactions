package com.example.bank.partner;

import com.example.bank.partner.model.Account;
import com.example.bank.partner.model.OverdraftException;
import com.example.bank.partner.model.PartnerMoneyTransfer;
import com.example.bank.partner.model.mapper.AccountRowMapper;
import com.example.bank.partner.model.mapper.PartnerMoneyTransferRowMapper;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Configuration
@SpringBootApplication
public class PartnerBankApplication {

	public static void main(String[] args) {
		SpringApplication.run(PartnerBankApplication.class, args);
	}

	@GetMapping
	public List<Account> getAccounts() {
		return jdbc().query("select * from london_account", new AccountRowMapper());
	}

	@GetMapping("/transfers")
	public List<PartnerMoneyTransfer> getMoneyTransfers() {
		return jdbc().query("select * from london_money_transfer", new PartnerMoneyTransferRowMapper());
	}

	// TODO add transfer from partners to our bank
	@PostMapping("/reserveMoney")
	public String reserveMoney(@RequestBody Map<String, String> request) throws OverdraftException {
		String transferId = UUID.randomUUID().toString();

		// TODO begin transaction manually (use transaction template and manual transaction management
		jdbc().update("INSERT INTO london_money_transfer (transfer_id, account, external_account, " +
			"amount, direction, status) VALUES (?, ?, ?, ?, ?, ?)",
				transferId, request.get("to"), request.get("from"),
				Integer.parseInt(request.get("amount")), "IN", "RESERVED"); // TODO add reservation time

		// TODO commit transaction manually
		// TODO run background process to cancel all transactions which older then 30 minutes

        return transferId;
	}

    @PostMapping("/confirmTransfer/{transferId}")
    public void confirmTransfer(@PathVariable String transferId) throws OverdraftException {
		// TODO begin transaction manually

		// Search and lock money transfer
		PartnerMoneyTransfer transfer = jdbc().queryForObject("SELECT * FROM london_money_transfer WHERE transfer_id = ? AND status = ? FOR UPDATE",
				new PartnerMoneyTransferRowMapper(), transferId, "RESERVED");

		// Search and lock account
		Account account = jdbc().queryForObject("SELECT * FROM london_account WHERE identifier=? FOR UPDATE",
				new AccountRowMapper(), transfer.getAccount());

		// No validation needed for incoming transfer, needed for outgoing transfer
		int updatedRows = jdbc().update("UPDATE london_money_transfer SET status = ? WHERE transfer_id = ? AND status = ?",
				"CONFIRMED", transferId, "RESERVED");
		// TODO assert updated rows

		int updatedAccRows = jdbc().update("UPDATE london_account SET balance = ? WHERE identifier = ?",
				account.getBalance() + transfer.getAmount(), account.getIdentifier());
		// TODO assert updated rows

		// TODO commit transaction manually
    }

    @PostMapping("/cancelTransfer/{transferId}")
    public void cancelTransfer(@PathVariable String transferId) throws OverdraftException {
		// TODO begin transaction manually

		int updatedRows = jdbc().update("UPDATE london_money_transfer SET status = ?, cancellation_reason = ? " +
                        "WHERE transfer_id = ? AND status = ?",
				"CANCELLED", "cancelled by web-service call", transferId, "RESERVED");
		// TODO assert updated rows

		// TODO commit transaction manually
    }

	@Bean
	public JdbcTemplate jdbc() {
		return new JdbcTemplate(dataSource());
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