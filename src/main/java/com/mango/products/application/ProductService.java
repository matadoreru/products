package com.mango.products.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mango.products.domain.Product;
import com.mango.products.domain.ProductNotFoundException;
import com.mango.products.infrastructure.persistence.ProductRepository;

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
}
