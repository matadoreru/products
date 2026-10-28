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
import java.util.List;

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
    public Price addPrice(Long productId, BigDecimal value, LocalDate initDate, @Nullable LocalDate endDate) {
        Product product = productService.getForUpdate(productId);
        Price price = new Price(product, value, initDate, endDate);

        boolean overlaps = priceRepository.findByProductIdOrderByInitDate(productId).stream()
            .anyMatch(existingPrice -> existingPrice.overlaps(initDate, endDate));

        if (overlaps) {
            throw new PriceOverlapException(productId, initDate, endDate);
        }

        try {
            return priceRepository.saveAndFlush(price);
        } catch (DataIntegrityViolationException e) {
            throw new PriceOverlapException(productId, initDate, endDate);
        }
    }

    @Transactional(readOnly = true)
    public Price findPriceAt(Long productId, LocalDate date) {
        return priceRepository.findApplicable(productId, date)
            .orElseThrow(() -> {
                productService.get(productId);
                return new PriceNotFoundException(productId, date);
            });
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

    private static Specification<Price> buildFilter(Long productId, PriceHistoryQuery query) {
        List<Specification<Price>> conditions = new ArrayList<>();
        conditions.add(PriceSpecifications.ofProduct(productId));
        if (query.from() != null) {
            conditions.add(PriceSpecifications.activeOnOrAfter(query.from()));
        }
        if (query.to() != null) {
            conditions.add(PriceSpecifications.activeOnOrBefore(query.to()));
        }
        return Specification.allOf(conditions);
    }
}
