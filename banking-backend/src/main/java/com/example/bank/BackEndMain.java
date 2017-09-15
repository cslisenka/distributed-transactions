package com.example.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
public class BackEndMain {

	public static void main(String[] args) {
		SpringApplication.run(BackEndMain.class, args);
	}

	// TODO create performance tests for XA and similar non-XA actions, use JMeter + Dynatrace
	// TODO calculate overhead of XA
	// TODO add cleanup task for atomikos logs
}