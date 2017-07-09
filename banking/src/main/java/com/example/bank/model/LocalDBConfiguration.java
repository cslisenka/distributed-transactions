package com.example.bank.model;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class LocalDBConfiguration {

//    @Bean
//    public PlatformTransactionManager localTransactionManager(
//            @Qualifier("localEntityManagerFactory") LocalContainerEntityManagerFactoryBean factory) {
//        return new JpaTransactionManager(factory.getObject());
//    }

    @Bean("localJdbc")
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(localDataSource());
    }

    @Primary
    @Bean("localDataSource")
    public DataSource localDataSource() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setPort(3306);
        dataSource.setServerName("localhost");
        dataSource.setUser("root");
        dataSource.setPassword("root");
        dataSource.setDatabaseName("bank1");
        return dataSource;
    }

//    @Bean(name = "localXADaraSource")
//    public DataSource localXADataSource() {
//        MysqlXADataSource xaDataSource = new MysqlXADataSource();
//        xaDataSource.setPort(3306);
//        xaDataSource.setServerName("localhost");
//        xaDataSource.setUser("root");
//        xaDataSource.setPassword("root");
//        xaDataSource.setDatabaseName("bank1");
//        return xaDataSource;
//    }
}
