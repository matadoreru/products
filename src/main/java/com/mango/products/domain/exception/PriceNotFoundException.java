package com.mango.products.domain.exception;

import java.time.LocalDate;
import java.util.Currency;

public class PriceNotFoundException extends RuntimeException {

    public PriceNotFoundException(Long productId, Currency currency, LocalDate date) {
        super("No price in %s found for product ID %d on date %s.".formatted(currency, productId, date));
    }
}
