package com.mango.products.application;

import com.mango.products.domain.exception.InvalidHistoryFilterException;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Currency;

public record PriceHistoryQuery(
    @Nullable LocalDate from,
    @Nullable LocalDate to,
    @Nullable Currency currency,
    @Nullable Integer page,
    @Nullable Integer size,
    Sort.Direction direction) {

    public PriceHistoryQuery {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidHistoryFilterException(from, to);
        }
    }

    public boolean isPaginated() {
        return page != null || size != null;
    }
}
