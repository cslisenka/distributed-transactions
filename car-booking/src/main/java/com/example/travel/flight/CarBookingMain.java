package com.example.travel.flight;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.springframework.beans.factory.InitializingBean;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Configuration
@SpringBootApplication
public class CarBookingMain {

    public static final String CONFIRMED = "CONFIRMED";
    public static final String RESERVED = "RESERVED";
    public static final String AVAILABLE = "AVAILABLE";

	public static void main(String[] args) {
		SpringApplication.run(CarBookingMain.class, args);
	}

	@PostMapping("/car/reserve")
	public String reserveCar(@RequestBody Map<String, String> rq) {
        String bookingId = "CAR-" + UUID.randomUUID().toString();

        int rowsUpdated = db().update("UPDATE available_cars SET traveller_name=?, days=?, car_booking_id=?, status=? WHERE status=? LIMIT 1",
                rq.get("travellerName"), rq.get("days"), bookingId, RESERVED, AVAILABLE);

        if (rowsUpdated < 1) {
            throw new RuntimeException("No available cars");
        }

        return bookingId;
	}

    @PostMapping("/car/{bookingId}/confirm")
    public void confirmCar(@PathVariable String bookingId) {
        int rowsUpdated = db().update("UPDATE available_cars SET status=? WHERE car_booking_id=?", CONFIRMED, bookingId);
        if (rowsUpdated < 1) {
            throw new RuntimeException("Can not confirm non-existed booking " + bookingId);
        }
	}

    @PostMapping("/car/{bookingId}/cancel")
    public void cancelCar(@PathVariable String bookingId) {
        int rowsUpdated = db().update("UPDATE available_cars SET status=?, traveller_name=null, days=null, car_booking_id=null WHERE car_booking_id=?", AVAILABLE, bookingId);
        if (rowsUpdated < 1) {
            throw new RuntimeException("Can not cancel non-existed booking " + bookingId);
        }
    }

    // Needed by XA resource
    @GetMapping("/car/unfinished")
    public List<String> getUnfinishedTransfers() {
		return db().queryForList("select car_booking_id from available_cars WHERE status = ?", String.class, RESERVED);
	}

	@Bean
	public JdbcTemplate db() {
		return new JdbcTemplate(dataSource());
	}

	@Bean
	public DataSource dataSource() {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setPort(3306);
		dataSource.setServerName("localhost");
		dataSource.setUser("root");
		dataSource.setPassword("root");
		dataSource.setDatabaseName("rental_cars");
		return dataSource;
	}

    @Bean
    public InitializingBean cleanupDB() {
        return () -> {
            db().update("DELETE FROM available_cars");
            for (int i = 0; i < 20; i++) {
                db().update("INSERT INTO available_cars (status) VALUES('AVAILABLE')");
            }
        };
    }
}