package com.mango.products.infrastructure.web.dto;

import com.mango.products.domain.model.Price;

import java.math.BigDecimal;

public record PriceValueResponse(BigDecimal value,
                                 String currency) {

    public static PriceValueResponse from(Price price) {
        return new PriceValueResponse(price.getValue(), price.getCurrency().getCurrencyCode());
    }
}
