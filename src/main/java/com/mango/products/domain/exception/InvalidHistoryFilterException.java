package com.mango.products.domain.exception;

import java.time.LocalDate;

public class InvalidHistoryFilterException extends RuntimeException {

    public InvalidHistoryFilterException(LocalDate from, LocalDate to) {
        super("Filter 'from' (%s) must not be after 'to' (%s).".formatted(from, to));
    }
}
