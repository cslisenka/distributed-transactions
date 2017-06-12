package com.slisenko.atomikos;

import com.atomikos.icatch.tcc.rest.CoordinatorImp;
import com.atomikos.icatch.tcc.rest.MimeTypes;
import com.atomikos.icatch.tcc.rest.TransactionProvider;
import com.atomikos.tcc.rest.ParticipantLink;
import com.atomikos.tcc.rest.Transaction;
import com.slisenko.atomikos.tcc.AustrianBookingService;
import com.slisenko.atomikos.tcc.BelaviaBookingService;
import junit.framework.Assert;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TCCExample {

    private static final String COORDINATOR_URL = "http://localhost:8000";
    private static final String BELAVIA_URL = "http://localhost:9001";
    private static final String AUSTRIAN_URL = "http://localhost:9002";

    private static final List participantsProviders = new ArrayList();
    private static final List coordinatorProviders = new ArrayList();

    public static void main(String[] args) throws InterruptedException, IOException {
        participantsProviders.add(new JacksonJsonProvider());
        coordinatorProviders.add(new JacksonJsonProvider());
        coordinatorProviders.add(new TransactionProvider());

        startEmbeddedServer(BELAVIA_URL, BelaviaBookingService.class, participantsProviders);
        startEmbeddedServer(AUSTRIAN_URL, AustrianBookingService.class, participantsProviders);
        startEmbeddedServer(COORDINATOR_URL, CoordinatorImp.class, coordinatorProviders);

        // Book in Belavia: PUT localhost:9001/booking/bookTicket (1)
        ParticipantLink p1 = bookTicket(1, BELAVIA_URL);
        // Book in Austrian: PUT localhost:9002/booking/bookTicket (1)
        ParticipantLink p2 = bookTicket(1, AUSTRIAN_URL);

        // Ask coordinator to commit transaction
        //      Confirm Belavia: PUT http://localhost:9001/booking/1,
        //      Confirm Austrian: PUT http://localhost:9002/booking/1,
        // TODO if both success
        confirm(p1, p2);

        // Ask coordinator to cancel both reservations
        //      Cancel Belavia: DELETE http://localhost:9001/booking/1,
        //      Cancel Austrian: DELETE http://localhost:9002/booking/1,
        // TODO if at least one fail
        cancel(p1, p2);
    }

    private static void startEmbeddedServer(String inventoryBaseUrl, Class<?> resourceClasses, List providers) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setProviders(providers);
        sf.setResourceClasses(resourceClasses);
        sf.setAddress(inventoryBaseUrl);
        sf.create();
    }

    private static ParticipantLink bookTicket(int bookingId, String url) throws JsonParseException, JsonMappingException, IOException {
        WebClient inventoryClient = WebClient.create(url, participantsProviders);
        Response resp1 = inventoryClient.path("/booking/bookTicket").accept("application/json")
                .type("application/json").put(bookingId);

        ObjectMapper mapper = new ObjectMapper();
        ParticipantLink participant = mapper.readValue((InputStream) resp1.getEntity(), ParticipantLink.class);
        System.out.println("ParticipantLink, URI: " + participant.getUri() + ", expires: " + participant.getExpires());
        return participant;
    }

    private static void confirm(ParticipantLink participant1, ParticipantLink participant2) {
        Transaction transaction = new Transaction();
        transaction.getParticipantLinks().add(participant1);
        transaction.getParticipantLinks().add(participant2);

        WebClient coordinator = WebClient.create(COORDINATOR_URL, coordinatorProviders);
        Response resp = coordinator.path("/coordinator/confirm").
                accept(MimeTypes.MIME_TYPE_COORDINATOR_JSON).
                type(MimeTypes.MIME_TYPE_COORDINATOR_JSON)
                .put(transaction);
        System.out.println(resp);
        Assert.assertEquals(204, resp.getStatus());
    }

    private static void cancel(ParticipantLink participant1, ParticipantLink participant2) {
        Transaction transaction = new Transaction();
        transaction.getParticipantLinks().add(participant1);
        transaction.getParticipantLinks().add(participant2);

        WebClient coordinator = WebClient.create(COORDINATOR_URL, coordinatorProviders);
        Response resp = coordinator.path("/coordinator/cancel").
                accept(MimeTypes.MIME_TYPE_COORDINATOR_JSON).
                type(MimeTypes.MIME_TYPE_COORDINATOR_JSON)
                .put(transaction);
        System.out.println(resp);
        Assert.assertEquals(204, resp.getStatus());
    }
}