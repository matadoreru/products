package com.mango.products.infrastructure.web.dto;

import com.mango.products.domain.Product;

public record ProductResponse(Long id, String name, String description) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription());
    }
}
