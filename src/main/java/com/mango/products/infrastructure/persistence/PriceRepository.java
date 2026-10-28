package com.mango.products.infrastructure.persistence;

import com.mango.products.domain.Price;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long> {

    @Query("""
        SELECT p FROM Price p
        WHERE p.product.id = :productId
          AND p.initDate <= :date
          AND (p.endDate IS NULL OR p.endDate >= :date)
        """)
    Optional<Price> findApplicable(@Param("productId") Long productId, @Param("date") LocalDate date);

    List<Price> findByProductIdOrderByInitDate(Long productId);

    List<Price> findByProductId(Long productId, Sort sort);

    Page<Price> findByProductId(Long productId, Pageable pageable);

}
