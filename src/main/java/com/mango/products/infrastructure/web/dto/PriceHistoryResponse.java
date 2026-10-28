package com.mango.products.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mango.products.application.PageInfo;
import com.mango.products.application.ProductPriceHistory;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record PriceHistoryResponse(
    String name,
    String description,
    List<PriceResponse> prices,
    @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable PageInfo page) {

    public static PriceHistoryResponse from(ProductPriceHistory history) {
        return new PriceHistoryResponse(
            history.product().getName(),
            history.product().getDescription(),
            history.prices().stream().map(PriceResponse::from).toList(),
            history.page());
    }
}
