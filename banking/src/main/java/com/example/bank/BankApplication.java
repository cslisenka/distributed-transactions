package com.example.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
public class BankApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankApplication.class, args);
	}

	// TODO create performance tests for XA and similar non-XA actions, use JMeter + Dynatrace
	// TODO calculate overhead of XA
	// TODO add cleanup task (if we passed flag as input parameter)
	// TODO cleanup should restore database into initial state + clear all TransactionManager logs
}