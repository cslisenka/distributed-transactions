package com.example.bank.integration.partner;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class PartnerDBConfiguration {

    @Bean(name = "partnerDataSource")
    public DataSource partnerDataSource() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setPort(3306);
        dataSource.setServerName("localhost");
        dataSource.setUser("root");
        dataSource.setPassword("root");
        dataSource.setDatabaseName("partner_bank");
        return dataSource;
    }

    @Bean(name = "partnerXADataSource")
    public DataSource partnerXADataSource() {
        MysqlXADataSource xaDataSource = new MysqlXADataSource();
        xaDataSource.setPort(3306);
        xaDataSource.setServerName("localhost");
        xaDataSource.setUser("root");
        xaDataSource.setPassword("root");
        xaDataSource.setDatabaseName("partner_bank");
        return xaDataSource;
    }
}