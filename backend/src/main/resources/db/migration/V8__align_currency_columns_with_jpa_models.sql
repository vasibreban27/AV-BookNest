-- JPA maps currency as String with length 3, which Hibernate validates as VARCHAR(3).
-- V5 originally created these columns as CHAR(3). Convert both mapped columns
-- without changing the already-applied migration or losing existing values.

ALTER TABLE orders
    ALTER COLUMN currency DROP DEFAULT,
    ALTER COLUMN currency TYPE VARCHAR(3) USING BTRIM(currency),
    ALTER COLUMN currency SET DEFAULT 'RON';

ALTER TABLE payments
    ALTER COLUMN currency DROP DEFAULT,
    ALTER COLUMN currency TYPE VARCHAR(3) USING BTRIM(currency),
    ALTER COLUMN currency SET DEFAULT 'RON';
