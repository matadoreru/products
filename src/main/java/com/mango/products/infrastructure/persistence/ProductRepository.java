package com.mango.products.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mango.products.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{


}
