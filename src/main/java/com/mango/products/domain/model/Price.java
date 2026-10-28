package com.mango.products.domain.model;

import com.mango.products.domain.exception.InvalidPriceRangeException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

@Entity
@Table(name = "price")
public class Price {

    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("EUR");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "init_date", nullable = false)
    private LocalDate initDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    protected Price() {
    }

    public Price(Product product, BigDecimal value, LocalDate initDate, LocalDate endDate, Currency currency) {
        if (endDate != null && !initDate.isBefore(endDate)) {
            throw new InvalidPriceRangeException(initDate, endDate);
        }

        this.product = product;
        this.value = value;
        this.initDate = initDate;
        this.endDate = endDate;
        this.currency = currency;
    }

    public boolean isActive(LocalDate date) {
        return !date.isBefore(initDate) && (endDate == null || !date.isAfter(endDate));
    }

    public boolean overlaps(LocalDate otherInitDate, LocalDate otherEndDate) {
        boolean startsBeforeOtherEnds = otherEndDate == null || !initDate.isAfter(otherEndDate);
        boolean otherStartsBeforeThisEnds = endDate == null || !otherInitDate.isAfter(endDate);
        return startsBeforeOtherEnds && otherStartsBeforeThisEnds;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getInitDate() {
        return initDate;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Product getProduct() {
        return product;
    }

    public Currency getCurrency() {
        return currency;
    }
}
