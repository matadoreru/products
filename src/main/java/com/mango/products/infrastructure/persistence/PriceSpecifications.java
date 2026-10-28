package com.mango.products.infrastructure.persistence;

import com.mango.products.domain.model.Price;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Currency;

public final class PriceSpecifications {

    private PriceSpecifications() {

    }

    public static Specification<Price> ofProduct(Long productId) {
        return (root, query, cb) -> cb.equal(root.get("product").get("id"), productId);
    }

    public static Specification<Price> activeOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("endDate")),
            cb.greaterThanOrEqualTo(root.<LocalDate>get("endDate"), from));
    }

    public static Specification<Price> activeOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("initDate"), to);
    }

    public static Specification<Price> inCurrency(Currency currency) {
        return (root, query, cb) -> cb.equal(root.get("currency"), currency);
    }
}
