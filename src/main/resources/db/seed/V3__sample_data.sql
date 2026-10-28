WITH product AS (
INSERT INTO product (name, description)
VALUES ('Zapatillas deportivas', 'Modelo 2025 edición limitada')
    RETURNING id
    )
INSERT INTO price (product_id, value, init_date, end_date)
SELECT product.id, v.value, v.init_date, v.end_date
FROM product, (VALUES
                   (99.99,  DATE '2024-01-01', DATE '2024-06-30'),
                   (129.99, DATE '2024-07-01', DATE '2024-12-31'),
                   (199.99, DATE '2025-01-01', NULL)
) AS v (value, init_date, end_date);

WITH product AS (
INSERT INTO product (name, description)
VALUES ('Camiseta básica', 'Algodón orgánico, manga corta')
    RETURNING id
    )
INSERT INTO price (product_id, value, init_date, end_date)
SELECT product.id, v.value, v.init_date, v.end_date
FROM product, (VALUES
                   (12.99, DATE '2024-01-01', DATE '2024-06-30'),
                   (9.99,  DATE '2024-07-01', DATE '2024-08-31'),
                   (14.99, DATE '2024-09-01', NULL)
) AS v (value, init_date, end_date);

WITH product AS (
INSERT INTO product (name, description)
VALUES ('Vaqueros slim fit', 'Denim elástico, tiro medio')
    RETURNING id
    )
INSERT INTO price (product_id, value, init_date, end_date)
SELECT product.id, v.value, v.init_date, v.end_date
FROM product, (VALUES
                   (39.99, DATE '2024-01-01', DATE '2024-12-31'),
                   (29.99, DATE '2025-01-01', DATE '2025-01-31'),
                   (45.99, DATE '2025-02-01', NULL)
) AS v (value, init_date, end_date);

WITH product AS (
INSERT INTO product (name, description)
VALUES ('Abrigo de lana', 'Colección otoño-invierno')
    RETURNING id
    )
INSERT INTO price (product_id, value, init_date, end_date)
SELECT product.id, v.value, v.init_date, v.end_date
FROM product, (VALUES
                   (149.99, DATE '2024-10-01', DATE '2025-02-28'),
                   (159.99, DATE '2025-10-01', NULL)
) AS v (value, init_date, end_date);

WITH product AS (
INSERT INTO product (name, description)
VALUES ('Vestido midi estampado', 'Nueva colección primavera')
    RETURNING id
    )
INSERT INTO price (product_id, value, init_date, end_date)
SELECT product.id, v.value, v.init_date, v.end_date
FROM product, (VALUES
                   (59.99, DATE '2025-04-01', NULL::date)
) AS v (value, init_date, end_date);

INSERT INTO product (name, description)
VALUES ('Bolso de mano', 'Próximamente, todavía sin precio');
