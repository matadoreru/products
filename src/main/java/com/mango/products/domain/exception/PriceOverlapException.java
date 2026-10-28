package com.mango.products.domain.exception;

import com.mango.products.domain.model.Price;

public class PriceOverlapException extends RuntimeException {

    public PriceOverlapException(Price price) {
        super("Price period [%s, %s] in %s overlaps with an existing price of product ID %d."
            .formatted(price.getInitDate(),
                price.getEndDate() == null ? "open-ended" : price.getEndDate(),
                price.getCurrency(),
                price.getProduct().getId()));
    }
}
