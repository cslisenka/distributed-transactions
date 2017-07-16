package com.slisenko.examples.test.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class TestMySQL {

    public static void main(String[] args) throws SQLException {
        System.out.println("Test distributed-tx-1");

        Connection con1 = DriverManager.getConnection("jdbc:mysql://localhost:3306/distributed-tx-1",
                "root","root");
        con1.setAutoCommit(false);

        Statement st1 = con1.createStatement();
        st1.executeUpdate("insert into my_table (value) values ('test')");
        con1.commit();

        System.out.println("Test distributed-tx-2");

        Connection con2 = DriverManager.getConnection("jdbc:mysql://localhost:3306/distributed-tx-2",
                "root","root");
        con2.setAutoCommit(false);

        Statement st2 = con2.createStatement();
        st2.executeUpdate("insert into my_table_2 (value) values ('test')");
        con2.commit();
    }
}