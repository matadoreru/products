package com.mango.products.infrastructure.web.dto;

import com.mango.products.application.ProductPriceHistory;

import java.util.List;

public record PriceHistoryResponse(String name, String description, List<PriceResponse> prices) {

    public static PriceHistoryResponse from(ProductPriceHistory history) {
        return new PriceHistoryResponse(
            history.product().getName(),
            history.product().getDescription(),
            history.prices().stream().map(PriceResponse::from).toList());
    }
}
