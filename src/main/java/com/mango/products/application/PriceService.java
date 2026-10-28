package com.mango.products.application;

import com.mango.products.domain.Price;
import com.mango.products.domain.PriceNotFoundException;
import com.mango.products.domain.PriceOverlapException;
import com.mango.products.domain.Product;
import com.mango.products.infrastructure.persistence.PriceRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    public ProductPriceHistory getHistory(Long productId, @Nullable Integer page,
                                          @Nullable Integer size, Sort.Direction direction) {
        Product product = productService.get(productId);
        Sort sort = Sort.by(direction, "initDate");

        if (page == null && size == null) {
            List<Price> prices = priceRepository.findByProductId(productId, sort);
            return new ProductPriceHistory(product, prices, null);
        }

        Pageable pageable = PageRequest.of(
            page == null ? 0 : page,
            size == null ? DEFAULT_PAGE_SIZE : size,
            sort);
        Page<Price> result = priceRepository.findByProductId(productId, pageable);
        return new ProductPriceHistory(product, result.getContent(), PageInfo.from(result));
    }
}
