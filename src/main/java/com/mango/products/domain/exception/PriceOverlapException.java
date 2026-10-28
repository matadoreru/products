package com.mango.products.domain.exception;

import java.time.LocalDate;

public class PriceOverlapException extends RuntimeException {

    public PriceOverlapException(Long productId, LocalDate initDate, LocalDate endDate) {
        super("Price period [%s, %s] overlaps with an existing price period for product ID %d."
            .formatted(initDate, endDate == null ? "open-ended" : endDate, productId));
    }
}
