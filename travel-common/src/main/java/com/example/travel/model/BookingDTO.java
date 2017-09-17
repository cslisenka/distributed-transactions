package com.example.travel.model;

import com.google.gson.Gson;
import org.springframework.jdbc.core.RowMapper;

import javax.jms.*;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class BookingDTO implements Serializable {

    private static final String IDENTIFIER = "IDENTIFIER";
    private static final String TRAVELLER_NAME = "TRAVELLER_NAME";
    private static final String TRAVEL_FROM = "TRAVEL_FROM";
    private static final String TRAVEL_TO = "TRAVEL_TO";
    private static final String NIGHTS = "NIGHTS";

    private String identifier;
    private String travellerName;
    private String travelFrom;
    private String travelTo;
    private int nights;

    private Collection<BookingItemDTO> items = new ArrayList<BookingItemDTO>();

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTravellerName() {
        return travellerName;
    }

    public void setTravellerName(String travellerName) {
        this.travellerName = travellerName;
    }

    public String getTravelFrom() {
        return travelFrom;
    }

    public void setTravelFrom(String travelFrom) {
        this.travelFrom = travelFrom;
    }

    public String getTravelTo() {
        return travelTo;
    }

    public void setTravelTo(String travelTo) {
        this.travelTo = travelTo;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public Collection<BookingItemDTO> getItems() {
        return items;
    }

    public void setItems(Collection<BookingItemDTO> items) {
        this.items = items;
    }

    public static class BookingRowMapper implements RowMapper<BookingDTO> {
        @Override
        public BookingDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            BookingDTO result = new BookingDTO();
            result.setIdentifier(rs.getString(IDENTIFIER));
            result.setTravelFrom(rs.getString(TRAVEL_FROM));
            result.setTravelTo(rs.getString(TRAVEL_TO));
            result.setNights(rs.getInt(NIGHTS));
            result.setTravellerName(rs.getString(TRAVELLER_NAME));
            return result;
        }
    }

    public TextMessage to(TextMessage msg) throws JMSException {
        msg.setText(new Gson().toJson(this));
        return msg;
    }

    public static BookingDTO from(Message msg) throws JMSException {
        if (msg instanceof TextMessage) {
            return new Gson().fromJson(((TextMessage) msg).getText(), BookingDTO.class);
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookingDTO that = (BookingDTO) o;

        return identifier != null ? identifier.equals(that.identifier) : that.identifier == null;
    }

    @Override
    public int hashCode() {
        return identifier != null ? identifier.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BookingDTO{" +
                "identifier='" + identifier + '\'' +
                ", travellerName='" + travellerName + '\'' +
                ", travelFrom='" + travelFrom + '\'' +
                ", travelTo='" + travelTo + '\'' +
                ", nights=" + nights +
                ", items=" + items +
                '}';
    }
}