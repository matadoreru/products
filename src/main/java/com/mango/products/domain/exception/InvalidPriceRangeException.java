package com.mango.products.domain.exception;

import java.time.LocalDate;

public class InvalidPriceRangeException extends RuntimeException {

    public InvalidPriceRangeException(LocalDate initDate, LocalDate endDate) {
        super("Start date (%s) must be before end date (%s).".formatted(initDate, endDate));
    }
}
