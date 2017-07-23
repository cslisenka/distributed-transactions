UPDATE bank1.from SET balance = 10000;
DELETE FROM bank1.money_transfer;
DELETE FROM bank1.partner_money_transfer;

UPDATE partner_bank.london_account SET balance = 10000;
DELETE FROM partner_bank.london_money_transfer;