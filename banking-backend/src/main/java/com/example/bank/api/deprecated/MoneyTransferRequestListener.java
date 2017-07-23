package com.example.bank.api.deprecated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Message;
import javax.jms.MessageListener;

// Depending on configuration this listener may run within XA or non-XA transaction
// TODO move comments into actual code
//@Component
public class MoneyTransferRequestListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MoneyTransferRequestListener.class);

//    @Autowired
    private MoneyTransferAPI transferAPI; // TODO move that code into separate service in future

    // TODO after getting messages redelivered several times, following message is printed by Atomikos
    // TODO Possible poison message detected - check https://www.atomikos.com/Documentation/PoisonMessage:
    // TODO see MessageConsumerSession.checkRedeliveryLimit() - this code only prints warning, but doesn't prevent from further redelovery?
    // TODO question - what to do if redelivery limit reached, how can we handle that? - route into specific queue and do manual investigation
    @Override
    public void onMessage(Message message) {
//        // here we are if a message is received; a transaction
//        // as been started before this method has been called.
//        // this is done for us by the MessageDrivenContainer...
//        try {
//            MapMessage map = (MapMessage) message;
//
//            logger.info("RECEIVED [transfer_id={}, from={}, to={}, amount={}]", map.getString("transfer_id"),
//                    map.getString("from"), map.getString("to"), map.getString("amount"));
//
//            Map<String, String> request = new HashMap<>();
//            request.put("transfer_id", map.getString("transfer_id"));
//            request.put("from", map.getString("from"));
//            request.put("to", map.getString("to"));
//            request.put("amount", map.getString("amount"));
//
//            // Consuming JMS message + modifying DB + calling web-service happens in single XA transaction
////            transferAPI.xaTransferMoneyToPartnerWS(request);
//            // Handle received payment request
////            localTransferService.doTransfer(
////                    map.getString("transfer_id"), map.getString("from"),
////                    map.getString("to"), map.getInt("amount"));
//
//        } catch (OverdraftException e) {
//            logger.error("{}", e.getMessage());
//            // TODO handle overdraft, may be mark transfer as failed in DB? or send to failed queue?
//            // TODO let it be separate catch-block in case we need to explicitly recover overdraft in future
//            e.printStackTrace();
//            throw new RuntimeException(e); // For cancelling the whole XA transaction
//        } catch (Exception e) {
//            logger.error("{}", e.getMessage());
//            e.printStackTrace();
//        }
    }
}