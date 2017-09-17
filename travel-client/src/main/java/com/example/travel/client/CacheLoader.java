package com.example.travel.client;

import com.example.travel.model.Constants;
import com.example.travel.model.BookingDTO;
import com.example.travel.model.BookingItemDTO;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CacheLoader {

    private static final Logger logger = LoggerFactory.getLogger(CacheLoader.class);

    @Autowired
    private HazelcastInstance cache;

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void loadDataToCache() {
        Map<String, BookingDTO> bookings = new HashMap<>();
        jdbc.query("select * from booking", new BookingDTO.BookingRowMapper())
                .forEach(booking -> bookings.put(booking.getIdentifier(), booking));;

        List<BookingItemDTO> items = jdbc.query("select * from booking_item", new BookingItemDTO.BookingItemRowMapper());
        items.forEach(item -> bookings.get(item.getBookingIdentifier()).getItems().add(item));

        cache.getList(Constants.BOOKING_FINISHED).addAll(bookings.values());
        logger.info("{} bookings and {} booking items loaded to cache", bookings.size(), items.size());
    }
}