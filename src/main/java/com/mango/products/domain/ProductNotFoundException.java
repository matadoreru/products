package com.mango.products.domain;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Producto con ID %s no encontrado".formatted(id));
    }
}
