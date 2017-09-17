package com.example.travel;

import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactory;
import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactoryImpl;
import ch.maxant.generic_jca_adapter.TransactionAssistant;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.example.travel.model.BookingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import java.util.UUID;

@RestController
public class TravelBackendAPI implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(TravelBackendAPI.class);

    @Autowired
    @Qualifier("flightService")
    protected HTTPClient flightService;

    @Autowired
    @Qualifier("carService")
    protected HTTPClient carService;

    @Autowired
    @Qualifier("agencyDB")
    private JdbcTemplate agency;

    @Autowired
    @Qualifier("hotelDB")
    private JdbcTemplate hotels;

    @Autowired
    @Qualifier("jmsTemplate")
    private JmsTemplate jms;

    @Autowired
    @Qualifier("responseQueue")
    private Queue responseQueue;

    @Autowired
    @Qualifier("xaTransactionManager")
    protected UserTransactionManager tm;

    @PostMapping("/backend/performBooking")
    public String bookAll(@RequestBody BookingDTO request) throws Exception {
        tm.begin(); // Start JTA transaction
        logger.info("Received WS-request {} transactionId={}", request, tm.getTransaction());
        try {
            performBooking(request);
            tm.commit();
            return request.getIdentifier();
        } catch (Exception e) {
            logger.error("Error processing WS-request {} ({})", request, e.getMessage());
            tm.rollback();
            throw e;
        }
    }

    // We are already in JTA transaction because we are using Atomikos JMS container
    @Override
    public void onMessage(Message message) {
        try {
            BookingDTO request = BookingDTO.from(message);
            logger.info("Received JMS-request {} transactionId={}", request, tm.getTransaction());
            performBooking(request);
        } catch (Exception e) {
            logger.error("Error processing JMS-request {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // Must be in a global XA transaction, otherwise SAGAs can be used
    protected void performBooking(BookingDTO rq) throws Exception {
        // Booking identifier
        String bookingId = UUID.randomUUID().toString();
        rq.setIdentifier(bookingId);

        // Storing booking in agency DB
        agency.update("INSERT INTO booking (identifier, traveller_name, travel_from, travel_to, nights) VALUES (?,?,?,?,?)",
                bookingId, rq.getTravellerName(), rq.getTravelFrom(), rq.getTravelTo(), rq.getNights());

        // Book hotel, update in hoteld DB
        int rowsUpdated = hotels.update("UPDATE available_rooms SET traveller_name=?, city=?, nights=?, room_booking_id=?, status=? WHERE status=? LIMIT 1",
                rq.getTravellerName(), rq.getTravelTo(), rq.getNights(), bookingId, "CONFIRMED", "AVAILABLE");

        if (rowsUpdated < 1) {
            throw new RuntimeException("No available rooms");
        }

        // Recording booking idem in agency DB
        agency.update("INSERT INTO booking_item (booking_identifier, type, details) VALUES (?, ?, ?)", bookingId, "HOTEL", bookingId);

        // Book flight, call flights web-service
        BasicTransactionAssistanceFactory transferServiceTransactionFactory = new BasicTransactionAssistanceFactoryImpl("xa/flightService");
        try (TransactionAssistant transactionAssistant = transferServiceTransactionFactory.getTransactionAssistant()) {
            String flightBookingId = transactionAssistant.executeInActiveTransaction(
                    xaTransactionId -> flightService.reserve(new FlightRequestDTO(rq), xaTransactionId));

            // Recording booking idem in agency DB
            agency.update("INSERT INTO booking_item (booking_identifier, type, details) VALUES (?, ?, ?)", bookingId, "FLIGHT", flightBookingId);
        }

        // Rent car, call cars web-service
        transferServiceTransactionFactory = new BasicTransactionAssistanceFactoryImpl("xa/carService");
        try (TransactionAssistant transactionAssistant = transferServiceTransactionFactory.getTransactionAssistant()) {
            String carBookingId = transactionAssistant.executeInActiveTransaction(
                    xaTransactionId -> carService.reserve(new CarRequestDTO(rq), xaTransactionId));

            // Recording booking item in agency DB
            agency.update("INSERT INTO booking_item (booking_identifier, type, details) VALUES (?, ?, ?)", bookingId, "CAR", carBookingId);
        }

        // Send response back via JMS
        jms.send(responseQueue, session -> rq.to(session.createTextMessage()));
    }
}