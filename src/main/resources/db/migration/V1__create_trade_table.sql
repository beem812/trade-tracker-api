--  CREATE EXTENSION IF NOT EXISTS "uuid-ossp"

CREATE TABLE wheel_trade (
  id VARCHAR(50) DEFAULT random_uuid() NOT NULL PRIMARY KEY, 
  ticker VARCHAR(10),
  action VARCHAR(50),
  date VARCHAR(50),
  price_per_share DECIMAL,
  shares INT,
  credit_debit VARCHAR(20)
);

INSERT INTO wheel_trade (ticker, date, action, price_per_share, shares, credit_debit)
VALUES ('CRSR', CURRENT_DATE(), 'CC', 3.95, 100, 'Credit')