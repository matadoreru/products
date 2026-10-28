package com.mango.products.domain.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Product with ID %s not found.".formatted(id));
    }
}
