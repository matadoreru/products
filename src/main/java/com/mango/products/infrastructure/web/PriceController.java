package com.mango.products.infrastructure.web;

import com.mango.products.application.PriceService;
import com.mango.products.domain.Price;
import com.mango.products.infrastructure.web.dto.CreatePriceRequest;
import com.mango.products.infrastructure.web.dto.PriceHistoryResponse;
import com.mango.products.infrastructure.web.dto.PriceResponse;
import com.mango.products.infrastructure.web.dto.PriceValueResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/products/{productId}/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @PostMapping
    public ResponseEntity<PriceResponse> addPrice(@PathVariable Long productId, @Valid @RequestBody CreatePriceRequest request) {
        Price price = priceService.addPrice(productId, request.value(), request.initDate(), request.endDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(PriceResponse.from(price));
    }

    @GetMapping(params = "date")
    public PriceValueResponse getPriceAt(@PathVariable Long productId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new PriceValueResponse(priceService.findPriceAt(productId, date).getValue());
    }

    @GetMapping(params = "!date")
    public PriceHistoryResponse getHistory(@PathVariable Long productId) {
        return PriceHistoryResponse.from(priceService.getHistory(productId));
    }
}
