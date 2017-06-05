package com.slisenko.examples.xa;

import com.slisenko.examples.xa.util.MyTransactionManager;

import javax.transaction.xa.XAException;
import java.sql.SQLException;
import java.sql.Statement;

public class TestOneSuccessOneFail {

    public static void main(String[] args) throws SQLException, XAException {
        MyTransactionManager.perform((c1, c2) -> {
            Statement st1 = c1.createStatement();
            Statement st2 = c2.createStatement();

            // Do updates
            st1.executeUpdate("insert into my_table (value) values ('not-null')");
            st2.executeUpdate("insert into my_table_2 (value) values (null)");
        });
    }
}