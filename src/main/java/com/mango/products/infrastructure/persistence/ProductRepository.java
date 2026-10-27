package com.mango.products.infrastructure.persistence;

import com.mango.products.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
