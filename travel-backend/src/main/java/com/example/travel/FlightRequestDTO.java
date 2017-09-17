package com.example.travel;

import com.example.travel.model.BookingDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

// TODO dates
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightRequestDTO implements Serializable {

    private String bookingId;
    private String travelFrom;
    private String travelTo;
    private String travellerName;

    public FlightRequestDTO() {
    }

    public FlightRequestDTO(BookingDTO booking) {
        this.bookingId = booking.getIdentifier();
        this.travelFrom = booking.getTravelFrom();
        this.travelTo = booking.getTravelTo();
        this.travellerName = booking.getTravellerName();
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
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

    public String getTravellerName() {
        return travellerName;
    }

    public void setTravellerName(String travellerName) {
        this.travellerName = travellerName;
    }

    @Override
    public String toString() {
        return "FlightRequestDTO{" +
                "bookingId='" + bookingId + '\'' +
                ", travelFrom='" + travelFrom + '\'' +
                ", travelTo='" + travelTo + '\'' +
                ", travellerName='" + travellerName + '\'' +
                '}';
    }
}
