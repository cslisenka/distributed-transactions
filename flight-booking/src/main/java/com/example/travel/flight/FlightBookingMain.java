package com.example.travel.flight;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.springframework.beans.factory.InitializingBean;
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
public class FlightBookingMain {

    public static final String CONFIRMED = "CONFIRMED";
    public static final String RESERVED = "RESERVED";
    public static final String AVAILABLE = "AVAILABLE";

	public static void main(String[] args) {
		SpringApplication.run(FlightBookingMain.class, args);
	}

	@PostMapping("/flight/reserve")
	public String reserveFlight(@RequestBody Map<String, String> rq) {
		String bookingId = "FLIGHT-" + UUID.randomUUID().toString();

        int rowsUpdated = db().update("UPDATE available_seats SET travel_to=?, travel_from=?, traveller_name=?, seat_booking_id=?, status=? WHERE status=? LIMIT 1",
                rq.get("travelTo"), rq.get("travelFrom"), rq.get("travellerName"), bookingId, RESERVED, AVAILABLE);

        if (rowsUpdated < 1) {
            throw new RuntimeException("No available seats");
        }

        return bookingId;
	}

    @PostMapping("/flight/{bookingId}/confirm")
    public void confirmFlight(@PathVariable String bookingId) {
        int rowsUpdated = db().update("UPDATE available_seats SET status=? WHERE seat_booking_id=?", CONFIRMED, bookingId);
        if (rowsUpdated < 1) {
            throw new RuntimeException("Can not confirm non-existed booking " + bookingId);
        }
	}

    @PostMapping("/flight/{bookingId}/cancel")
    public void cancelFlight(@PathVariable String bookingId) {
        int rowsUpdated = db().update("UPDATE available_seats SET status=?, travel_to=null, travel_from=null, traveller_name=null, seat_booking_id=null WHERE seat_booking_id=?", AVAILABLE, bookingId);
        if (rowsUpdated < 1) {
            throw new RuntimeException("Can not cancel non-existed booking " + bookingId);
        }
	}

    // Needed by XA resource
    @GetMapping("/flight/unfinished")
    public List<String> getUnfinishedTransfers() {
		return db().queryForList("select seat_booking_id from available_seats WHERE status = ?", String.class, RESERVED);
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
		dataSource.setDatabaseName("flights");
		return dataSource;
	}

	@Bean
	public InitializingBean cleanupDB() {
	    return () -> {
	        db().update("DELETE FROM available_seats");
            for (int i = 0; i < 20; i++) {
                db().update("INSERT INTO available_seats (status) VALUES('AVAILABLE')");
            }
        };
    }
}