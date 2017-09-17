package com.example.travel;

import com.example.travel.model.BookingDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CarRequestDTO implements Serializable {

    private String travellerName;
    private int days;

    public CarRequestDTO() {
    }

    public CarRequestDTO(BookingDTO booking) {
        this.travellerName = booking.getTravellerName();
        this.days = booking.getNights();
    }

    public String getTravellerName() {
        return travellerName;
    }

    public void setTravellerName(String travellerName) {
        this.travellerName = travellerName;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    @Override
    public String toString() {
        return "CarRequestDTO{" +
                "travellerName='" + travellerName + '\'' +
                ", days=" + days +
                '}';
    }
}
