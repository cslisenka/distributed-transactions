package com.example.bank.api.deprecated;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class BackendAPI { //} implements MessageListener {

//    private static final Logger logger = LoggerFactory.getLogger(BackendAPI.class);
//
//    @Autowired
//    private HTTPClient ws;
//
//    @Autowired
//    @Qualifier("nonxa")
//    private JdbcTemplate nonxa;
//
//    @Autowired
//    @Qualifier("partner")
//    private JdbcTemplate partner;
//
//    @Autowired
//    @Qualifier("xaJmsTemplate")
//    private JmsTemplate jms;
//
//    @Autowired
//    @Qualifier("cacheUpdateQueue")
//    private Queue cacheUpdateQueue;
//
//    @Autowired
//    private UserTransactionManager tm;
//
//    @PostMapping("/transfer")
//    public String xaTransferMoney(@RequestBody MoneyTransferDTO request) throws Exception {
//        tm.begin(); // Start JTA transaction
//        logger.info("Received WS-request {} transactionId={}", request, tm.getTransaction());
//        try {
//            String transferID = UUID.randomUUID().toString();
//            request.setTransferId(transferID);
//            doTransfer(request);
//            tm.commit();
//            return transferID;
//        } catch (Exception e) {
//            logger.error("Error processing WS-request {} ({})", request, e.getMessage());
//            tm.rollback();
//            throw e;
//        }
//    }
//
//    // We are already in JTA transaction because we are using Atomikos JMS container
//    @Override
//    public void onMessage(Message message) {
//        try {
//            MapMessage map = (MapMessage) message;
//            MoneyTransferDTO request = MoneyTransferDTO.from(map);
//            logger.info("Received JMS-request {} transactionId={}", request, tm.getTransaction());
//            doTransfer(request);
//        } catch (Exception e) {
//            logger.error("Error processing JMS-request {}", e.getMessage(), e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void doTransfer(MoneyTransferDTO request) throws Exception {
//        AccountDTO nonxa = withdrawLocalDB(request);
//        sendCacheUpdate(nonxa, request);
//
//        String bank = request.getToBank();
//        if ("nonxa".equals(bank)) {
//            AccountDTO accountTo = depositLocalDB(request);
//            sendCacheUpdate(accountTo, request);
//        } else if ("partner".equals(bank)) {
//            depositPartnerDB(request);
//        } else if ("external".equals(bank)) {
//            depositExternalWSXA(request);
//        }
//    }
//
//    private AccountDTO withdrawLocalDB(MoneyTransferDTO request) throws OverdraftException {
//        // Lock nonxa account
//        AccountDTO account = nonxa.queryForObject("select * from account where identifier = ? for update",
//                new AccountDTO.BookingRowMapper(), request.getTravelFrom());
//
//        // Only check for overdraft if we transfer money from nonxa account
//        if (account.getBalance() < request.getAmount()) {
//            throw new OverdraftException(request.getTravelFrom());
//        }
//
//        // Change balance nonxa account + save transfer in nonxa DB
//        nonxa.update("UPDATE account SET balance = ? WHERE identifier = ?",
//                account.getBalance() - request.getAmount(), request.getTravelFrom());
//
//        Date time = new Date();
//        nonxa.update("insert into money_transfer (transfer_id, account_from, account_to, bank_to, amount, date_time) " +
//                "values (?, ?, ?, ?, ?, ?)", request.getTransferId(), request.getTravelFrom(), request.getTravelTo(), "partner", request.getAmount(), time);
//
//        // Sending cache update event
//        account.setBalance(account.getBalance() - request.getAmount());
//        request.setDateTime(time);
//        return account;
//    }
//
//    private AccountDTO depositLocalDB(MoneyTransferDTO request) {
//        AccountDTO accountTo = nonxa.queryForObject("select * from account where identifier = ? for update",
//                new AccountDTO.BookingRowMapper(), request.getTravelTo());
//
//        nonxa.update("update account set balance = ? where identifier = ?",
//                accountTo.getBalance() + request.getAmount(), request.getTravelFrom());
//
//        accountTo.setBalance(accountTo.getBalance() + request.getAmount());
//        return accountTo;
//    }
//
//    private void depositPartnerDB(MoneyTransferDTO request) throws OverdraftException {
//        // Query for locking
//        AccountDTO partnerAccount = partner.queryForObject("select * from partner_account where identifier = ? for update",
//                new AccountDTO.BookingRowMapper(), request.getTravelTo());
//
//        // Change balance partner account
//        partner.update("UPDATE partner_account SET balance = ? WHERE identifier = ?",
//                partnerAccount.getBalance() + request.getAmount(), request.getTravelTo());
//
//        // save transfer in partner DB
//        partner.update("INSERT INTO partner_money_transfer (transfer_id, account, external_account, amount, status, date_time) " +
//                "VALUES (?, ?, ?, ?, ?, ?)", request.getTransferId(), request.getTravelTo(), request.getTravelFrom(), request.getAmount(), "CONFIRMED", new Date());
//    }
//
//    private void depositExternalWSXA(MoneyTransferDTO request) throws Exception {
//        // For demonstration purposes we can shut down partner web-services and show that transaction was rolled back (if our SQL is first)
//        // If our database (next operation) rejects payment - the cancel should be called on web-service
//        // TransactionManager will hold XA transaction on MySQL and JMS side and repeat retries to WS-confirm until timeout is passed, then call WS-cancel
//        BasicTransactionAssistanceFactory transferServiceTransactionFactory = new BasicTransactionAssistanceFactoryImpl("xa/transferService"); // TODO how to avoid using JNDI?
//        try (TransactionAssistant transactionAssistant = transferServiceTransactionFactory.getTransactionAssistant()){
//            String partnerTransferId = transactionAssistant.executeInActiveTransaction(xaTransactionId -> {
//                // In case of failure - TransactionManager must call WS-reject
//                return ws.reserveMoney(request.getTravelFrom(), request.getTravelTo(), request.getAmount(), xaTransactionId);
//            });
//
//            // TODO save it in our database may be
//            logger.info("Partner transfer id = {}", partnerTransferId);
//        }
//    }
//
//    private void sendCacheUpdate(AccountDTO account, MoneyTransferDTO transfer) {
//        jms.send(cacheUpdateQueue, session -> {
//            MapMessage msg = session.createMapMessage();
//            account.to(msg);
//            transfer.to(msg);
//            return msg;
//        });
//    }
}