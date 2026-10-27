package com.mango.products.domain;

import java.time.LocalDate;

public class PriceNotFoundException extends RuntimeException {

    public PriceNotFoundException(Long productId, LocalDate date) {
        super("No price found for product ID %d on date %s.".formatted(productId, date));
    }
}
