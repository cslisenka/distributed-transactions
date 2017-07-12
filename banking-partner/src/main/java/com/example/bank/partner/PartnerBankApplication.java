package com.example.bank.partner;

import com.example.bank.partner.model.Account;
import com.example.bank.partner.model.MoneyTransfer;
import com.example.bank.partner.model.OverdraftException;
import com.example.bank.partner.model.PartnerMoneyTransfer;
import com.example.bank.partner.model.mapper.AccountRowMapper;
import com.example.bank.partner.model.mapper.MoneyTransferRowMapper;
import com.example.bank.partner.model.mapper.PartnerMoneyTransferRowMapper;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

	@Autowired
	private JdbcTemplate jdbc;

	@GetMapping
	public List<Account> getAccounts() {
		return jdbc.query("select * from london_account", new AccountRowMapper());
	}

	@GetMapping("/transfers")
	public List<MoneyTransfer> getMoneyTransfers() { //@PathVariable String accountIdentifier
		return jdbc.query("select * from london_money_transfer", new MoneyTransferRowMapper());
	}

	@GetMapping("/partnerTransfers")
	public List<PartnerMoneyTransfer> getPartnerMoneyTransfers() {
		return jdbc.query("select * from london_partner_money_transfer", new PartnerMoneyTransferRowMapper());
	}

	@PostMapping("/transferMoneyLocal")
	public void transferMoney(@RequestBody Map<String, String> request) throws OverdraftException {
		String transferId = UUID.randomUUID().toString();
//		transferService.doTransfer(transferId, request.get("from"),
//				request.get("to"), Integer.parseInt(request.get("amount")));
	}

	@Bean
	public JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource());
	}

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