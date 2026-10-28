package com.mango.products.application;

import com.mango.products.domain.exception.PriceNotFoundException;
import com.mango.products.domain.exception.PriceOverlapException;
import com.mango.products.domain.model.Price;
import com.mango.products.domain.model.Product;
import com.mango.products.infrastructure.persistence.PriceRepository;
import com.mango.products.infrastructure.persistence.PriceSpecifications;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Service
public class PriceService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final PriceRepository priceRepository;
    private final ProductService productService;

    public PriceService(PriceRepository priceRepository, ProductService productService) {
        this.priceRepository = priceRepository;
        this.productService = productService;
    }

    @Transactional
    public Price addPrice(Long productId, BigDecimal value, Currency currency,
                          LocalDate initDate, @Nullable LocalDate endDate) {
        Product product = productService.getForUpdate(productId);
        return saveWithoutOverlap(new Price(product, value, initDate, endDate, currency));
    }

    @Transactional
    public Price updatePrice(Long productId, Long priceId, BigDecimal value, Currency currency,
                             LocalDate initDate, @Nullable LocalDate endDate) {
        productService.getForUpdate(productId);
        Price price = priceRepository.findByIdAndProductId(priceId, productId)
            .orElseThrow(() -> new PriceNotFoundException(productId, priceId));
        price.update(value, initDate, endDate, currency);
        return saveWithoutOverlap(price);
    }

    @Transactional
    public void deletePrice(Long productId, Long priceId) {
        priceRepository.delete(getPrice(productId, priceId));
    }

    @Transactional(readOnly = true)
    public Price findPriceAt(Long productId, Currency currency, LocalDate date) {
        Optional<Price> price = priceRepository.findApplicable(productId, currency, date);
        if (price.isPresent()) {
            return price.get();
        }
        // Tell an unknown product apart from a product without a price on that date.
        productService.ensureExists(productId);
        throw new PriceNotFoundException(productId, currency, date);
    }

    @Transactional(readOnly = true)
    public Price getPrice(Long productId, Long priceId) {
        Optional<Price> price = priceRepository.findByIdAndProductId(priceId, productId);
        if (price.isPresent()) {
            return price.get();
        }
        productService.ensureExists(productId);
        throw new PriceNotFoundException(productId, priceId);
    }

    @Transactional(readOnly = true)
    public ProductPriceHistory getHistory(Long productId, PriceHistoryQuery query) {
        Product product = productService.get(productId);
        Specification<Price> filter = buildFilter(productId, query);
        Sort sort = Sort.by(query.direction(), "initDate");

        if (!query.isPaginated()) {
            return new ProductPriceHistory(product, priceRepository.findAll(filter, sort), null);
        }

        Pageable pageable = PageRequest.of(
            query.page() == null ? 0 : query.page(),
            query.size() == null ? DEFAULT_PAGE_SIZE : query.size(),
            sort);
        Page<Price> result = priceRepository.findAll(filter, pageable);
        return new ProductPriceHistory(product, result.getContent(), PageInfo.from(result));
    }

    /**
     * Saves the price unless it overlaps another price of the same product and currency.
     * <p>
     * The caller holds the product lock, so the in-memory check is reliable and conflicts are
     * normally reported without hitting the database. The exclusion constraint remains as a safety
     * net: Hibernate may flush a pending update before the overlap query runs, so a violation can
     * surface from either statement and both are reported as the same conflict.
     */
    private Price saveWithoutOverlap(Price price) {
        try {
            if (overlapsAnotherPrice(price)) {
                throw new PriceOverlapException(price);
            }
            return priceRepository.saveAndFlush(price);
        } catch (DataIntegrityViolationException e) {
            throw new PriceOverlapException(price);
        }
    }

    private boolean overlapsAnotherPrice(Price price) {
        Long productId = price.getProduct().getId();
        return priceRepository.findByProductIdAndCurrency(productId, price.getCurrency()).stream()
            .filter(other -> !other.getId().equals(price.getId())) // a new price has no id yet
            .anyMatch(other -> other.overlaps(price.getInitDate(), price.getEndDate()));
    }

    private static Specification<Price> buildFilter(Long productId, PriceHistoryQuery query) {
        List<Specification<Price>> conditions = new ArrayList<>();
        conditions.add(PriceSpecifications.ofProduct(productId));

        if (query.from() != null) {
            conditions.add(PriceSpecifications.activeOnOrAfter(query.from()));
        }
        if (query.to() != null) {
            conditions.add(PriceSpecifications.activeOnOrBefore(query.to()));
        }
        if (query.currency() != null) {
            conditions.add(PriceSpecifications.inCurrency(query.currency()));
        }

        return Specification.allOf(conditions);
    }
}
