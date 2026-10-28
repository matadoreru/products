package com.mango.products.infrastructure.persistence;

import com.mango.products.domain.model.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long>, JpaSpecificationExecutor<Price> {

    @Query("""
        SELECT p FROM Price p
        WHERE p.product.id = :productId
          AND p.currency = :currency
          AND p.initDate <= :date
          AND (p.endDate IS NULL OR p.endDate >= :date)
        """)
    Optional<Price> findApplicable(@Param("productId") Long productId,
                                   @Param("currency") Currency currency,
                                   @Param("date") LocalDate date);

    List<Price> findByProductIdAndCurrency(Long productId, Currency currency);
}
