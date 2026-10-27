CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE product (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(1000)
);

CREATE TABLE price (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT        NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    value      NUMERIC(12,2) NOT NULL CHECK (value > 0),
    init_date  DATE          NOT NULL,
    end_date   DATE,
    CONSTRAINT price_valid_range CHECK (end_date IS NULL OR init_date < end_date),
    CONSTRAINT price_no_overlap EXCLUDE USING gist (
        product_id WITH =,
        daterange(init_date, end_date, '[]') WITH &&
    )
);

CREATE INDEX price_product_init_idx ON price (product_id, init_date);
