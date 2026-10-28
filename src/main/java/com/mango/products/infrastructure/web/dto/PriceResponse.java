package com.mango.products.infrastructure.web.dto;

import com.mango.products.domain.model.Price;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceResponse(BigDecimal value, LocalDate initDate, @Nullable LocalDate endDate) {

    public static PriceResponse from(Price price) {
        return new PriceResponse(price.getValue(), price.getInitDate(), price.getEndDate());
    }
}
