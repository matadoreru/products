ALTER TABLE price
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'EUR';

ALTER TABLE price
    ADD CONSTRAINT price_currency_format CHECK (currency ~ '^[A-Z]{3}$');

ALTER TABLE price DROP CONSTRAINT price_no_overlap;

ALTER TABLE price
    ADD CONSTRAINT price_no_overlap EXCLUDE USING gist (
        product_id WITH =,
        currency WITH =,
        daterange(init_date, end_date, '[]') WITH &&
    );

DROP INDEX price_product_init_idx;

CREATE INDEX price_product_currency_init_idx ON price (product_id, currency, init_date);
