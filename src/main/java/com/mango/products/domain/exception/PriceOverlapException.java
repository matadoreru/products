package com.mango.products.domain.exception;

import java.time.LocalDate;
import java.util.Currency;

public class PriceOverlapException extends RuntimeException {

    public PriceOverlapException(Long productId, Currency currency, LocalDate initDate, LocalDate endDate) {
        super("Price period [%s, %s] in %s overlaps with an existing price of product ID %d."
            .formatted(initDate, endDate == null ? "open-ended" : endDate, currency, productId));
    }
}
