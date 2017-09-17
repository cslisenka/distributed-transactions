package com.example.travel.model;

import org.springframework.jdbc.core.RowMapper;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingItemDTO implements Serializable{

    private static final String BOOKING_IDENTIFIER = "booking_identifier";
    private static final String DETAILS = "details";
    private static final String TYPE = "type";
    private static final String ID = "id";

    private int id;
    private String bookingIdentifier;
    private String type;
    private String details;

    public String getBookingIdentifier() {
        return bookingIdentifier;
    }

    public void setBookingIdentifier(String bookingIdentifier) {
        this.bookingIdentifier = bookingIdentifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static class BookingItemRowMapper implements RowMapper<BookingItemDTO> {
        @Override
        public BookingItemDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            BookingItemDTO result = new BookingItemDTO();
            result.setBookingIdentifier(rs.getString(BOOKING_IDENTIFIER));
            result.setDetails(rs.getString(DETAILS));
            result.setType(rs.getString(TYPE));
            result.setId(rs.getInt(ID));
            return result;
        }
    }

    @Override
    public String toString() {
        return "BookingItemDTO{" +
                "id=" + id +
                ", bookingIdentifier='" + bookingIdentifier + '\'' +
                ", type='" + type + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}