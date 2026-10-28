package com.mango.products.application;

import com.mango.products.domain.exception.ProductNotFoundException;
import com.mango.products.domain.model.Product;
import com.mango.products.infrastructure.persistence.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product create(String name, String description) {
        return productRepository.save(new Product(name, description));
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public void ensureExists(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
    }

    /**
     * Loads the product holding a row lock until the surrounding transaction ends, so that
     * writes to the prices of the same product are serialised. Must be called inside a transaction.
     */
    Product getForUpdate(Long id) {
        return productRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
