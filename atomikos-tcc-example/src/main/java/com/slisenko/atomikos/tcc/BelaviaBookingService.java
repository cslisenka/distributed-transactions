package com.slisenko.atomikos.tcc;

import com.atomikos.tcc.rest.ParticipantLink;

import javax.ws.rs.*;
import java.util.Calendar;

@Path("/booking")
public class BelaviaBookingService {

    @Produces("application/json")
    @Consumes("application/json")
    @PUT
    @Path("bookTicket")
    public ParticipantLink bookTicket(int ticketId) {
        // Doing reservation in ticketing system
        // Ticket still in PENDING state, not sold, but ot available for customers
        // TODO Ticket must be automatically cancelled by timeout

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, 40);

        return new ParticipantLink("http://localhost:9001/booking/" + ticketId, c.getTimeInMillis());
    }

    // TODO why void?
    @Consumes("application/tcc") // Special mime-type for tcc
    @PUT
    @Path("/{id}")
    public void confirm(@PathParam("id") String ticketId) {
        // Mark ticket as sold
        System.out.println("Belavia confirming ticket " + ticketId);
    }

    @Consumes("application/tcc")
    @DELETE
    @Path("/{id}")
    public void cancel(@PathParam("id") String ticketId) {
        // Doing ticket cancellation
        System.out.println("Belavia cancelling ticket " + ticketId);
    }
}