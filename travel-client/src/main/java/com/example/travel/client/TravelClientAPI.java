package com.example.travel.client;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.example.travel.model.Constants;
import com.example.travel.model.BookingDTO;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.transaction.HazelcastXAResource;
import com.hazelcast.transaction.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@RestController
public class TravelClientAPI implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(CacheLoader.class);

    @Autowired
    private HazelcastInstance cache;

    @Autowired
    private JmsTemplate jms;

    @Autowired
    private Queue requestQueue;

    @Autowired
    private UserTransactionManager transactionManager;

    @GetMapping("/booking/finished")
    public List<BookingDTO> getFinishedBookings() {
        return cache.getList(Constants.BOOKING_FINISHED);
    }

    @GetMapping("/booking/inprogress")
    public List<BookingDTO> getBookingsInProgress() {
        return cache.getList(Constants.BOOKING_IN_PROGRESS);
    }

    @PostMapping("/performBooking")
    public String performBooking(@RequestBody BookingDTO request) throws SystemException, NotSupportedException {
        transactionManager.begin();
        try {
            // This may be needed for high performance as well if we want to temporary not accept new payments for DB maintenance
            String bookingId = UUID.randomUUID().toString(); // UUID to make request idempotent
            request.setIdentifier(bookingId);

            // Send JMS message
            jms.send(requestQueue, session -> request.to(session.createTextMessage()));

            // Update cache
            doInTransaction(cacheTx -> cacheTx.getList(Constants.BOOKING_IN_PROGRESS).add(request));

            // Log
            logger.info("Sending {} to queue", request);

            transactionManager.commit();
            return bookingId;
        } catch (Exception e) {
            transactionManager.rollback();
            logger.error("Error sending {} ({})", request, e.getMessage());
            return e.getMessage();
        }
    }

    // Should be already in XA transaction
    @Override
    public void onMessage(Message message) {
        MapMessage map = (MapMessage) message;
        try {
            BookingDTO booking = BookingDTO.from(map);

            // Update cache with new booking and new money transfer
            doInTransaction(cacheTx -> {
                cacheTx.getList(Constants.BOOKING_IN_PROGRESS).remove(booking);
                cacheTx.getList(Constants.BOOKING_FINISHED).add(booking);
            });

            logger.info("Updated cache {}", booking);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error updating cache {} ({})", message, e.getMessage());
            throw new RuntimeException(e); // Cause transaction rollback
        }
    }

    private void doInTransaction(Consumer<TransactionContext> action) throws Exception {
        Transaction tx = transactionManager.getTransaction();
        HazelcastXAResource xaCacheResource = cache.getXAResource();
        tx.enlistResource(xaCacheResource);
        TransactionContext cacheTx = xaCacheResource.getTransactionContext();

        // Executing business logic
        action.accept(cacheTx);

        // TODO when should we delist resource? Just before commit, or after we completed all operations with him?
        // TODO check XA specification
        tx.delistResource(xaCacheResource, XAResource.TMSUCCESS);
    }
}