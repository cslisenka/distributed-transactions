package com.example.bank.api.deprecated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@RestController
public class MoneyTransferAPI {

    private static final Logger logger = LoggerFactory.getLogger(MoneyTransferAPI.class);

    // TODO move comments into actual code
//
//    @PostMapping("/xaTransferMoneyToPartnerWS")
//    public void xaTransferMoneyToPartnerWS(@RequestBody Map<String, String> request) throws Exception {
//        // If transaction is already started higher (in JMS listener) - the code must be commented
//        // If not commented - this method will be executed within nested transaction
//        // TODO change to txManager
////        UserTransaction tx = context.getBean(UserTransactionImp.class); // JTA transaction
////        tx.begin();
//        try {
//
//            // For demonstration reasons we can do request with account out of money or non-existing account (if SQL executed after WS-call - in this case TM triggers WS-reject)
//            // Doing local transfer
//            String transferId = UUID.randomUUID().toString();
////            xaSQLTransferService.doTransferLocal(transferId, request.get("from"),
////                    partnerTransferId, request.get("to"), Integer.parseInt(request.get("amount")));
//            xaSQLTransferService.doTransferLocal(transferId, request.get("from"),
//                    "none", request.get("to"), Integer.parseInt(request.get("amount")));
//
//            String partnerTransferId = null;
//
//            // For demonstration purposes we can shut down partner web-services and show that transaction was rolled back (if our SQL is first)
//            // If our database (next operation) rejects payment - the cancel should be called on web-service
//            // TransactionManager will hold XA transaction on MySQL side and repeat retries to WS-confirm until timeout is passed, then call WS-cancal
//            BasicTransactionAssistanceFactory transferServiceTransactionFactory = new BasicTransactionAssistanceFactoryImpl("xa/transferService"); // TODO how to avoid using JNDI?
//            try (TransactionAssistant transactionAssistant = transferServiceTransactionFactory.getTransactionAssistant()){
//                partnerTransferId = transactionAssistant.executeInActiveTransaction(xaTransactionId -> {
//                    // In case of failure - TransactionManager must call WS-reject
//                    return httpService.reserveMoney(request.get("from"), request.get("to"), Integer.parseInt(request.get("amount")), xaTransactionId);
//                });
//            }
//
////            tx.commit();
//        } catch (Exception e) {
//            logger.error("Failed XA (DB+WS) transfer, exception={}", e.getMessage());
////            tx.rollback();
//            throw e;
//        }
//
//        // TODO add web-page for visualizing recovery logs for Atomikos and our WS XA adapter - just print all files we have
//    }
}