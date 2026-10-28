package com.mango.products.infrastructure.web;

import com.mango.products.application.PriceHistoryQuery;
import com.mango.products.application.PriceService;
import com.mango.products.domain.model.Price;
import com.mango.products.infrastructure.web.dto.PriceHistoryResponse;
import com.mango.products.infrastructure.web.dto.PriceRequest;
import com.mango.products.infrastructure.web.dto.PriceResponse;
import com.mango.products.infrastructure.web.dto.PriceValueResponse;
import com.mango.products.infrastructure.web.validation.IsoCurrency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Currency;

@Tag(name = "Prices", description = "Manage the price history of a product")
@RestController
@RequestMapping("/products/{productId}/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @Operation(summary = "Add a price to a product",
        description = "Prices of the same product and currency cannot overlap. endDate is inclusive and can be null "
            + "for an open-ended price. currency is optional and defaults to EUR.")
    @ApiResponse(responseCode = "201", description = "Price created. The Location header points to the new price.")
    @ApiResponse(responseCode = "400", description = "Invalid body or currency code, or initDate is not before endDate")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409",
        description = "The price overlaps an existing price of the product in the same currency")
    @PostMapping
    public ResponseEntity<PriceResponse> addPrice(@PathVariable Long productId,
                                                  @Valid @RequestBody PriceRequest request) {
        Price price = priceService.addPrice(productId, request.value(), toCurrency(request.currency()),
            request.initDate(), request.endDate());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{priceId}")
            .buildAndExpand(price.getId())
            .toUri();

        return ResponseEntity.created(location).body(PriceResponse.from(price));
    }

    @Operation(summary = "Get the price active on a date",
        description = "With the date parameter, returns the price active on that date in the given currency, "
            + "EUR by default. Without it, returns the price history.")
    @ApiResponse(responseCode = "200", description = "Active price found")
    @ApiResponse(responseCode = "400", description = "Invalid date format or currency code")
    @ApiResponse(responseCode = "404", description = "Product not found, or no price in that currency on that date")
    @GetMapping(params = "date")
    public PriceValueResponse getPriceAt(
        @PathVariable Long productId,
        @Parameter(description = "Date in ISO format. Optional: if omitted, the price history is returned.",
            example = "2024-04-15")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @Parameter(description = "ISO 4217 currency code")
        @RequestParam(defaultValue = Price.DEFAULT_CURRENCY_CODE) @IsoCurrency String currency) {

        return PriceValueResponse.from(priceService.findPriceAt(productId, Currency.getInstance(currency), date));
    }

    @Operation(summary = "Get the price history of a product",
        description = "Optional filters by date range. Without page or size, returns all matching prices. "
            + "With any of them, returns one page.")
    @ApiResponse(responseCode = "200", description = "Product with its prices, ordered by initDate")
    @ApiResponse(responseCode = "400", description = "Invalid page, size, order or date range")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping(params = "!date")
    public PriceHistoryResponse getHistory(
        @PathVariable Long productId,
        @Parameter(description = "Only prices still active on or after this date", example = "2024-06-01")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
        @Parameter(description = "Only prices starting on or before this date", example = "2024-08-31")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to,
        @Parameter(description = "Only prices in this ISO 4217 currency", example = "EUR")
        @RequestParam(required = false) @IsoCurrency @Nullable String currency,
        @Parameter(description = "Page number, starting at 0")
        @RequestParam(required = false) @Min(0) @Nullable Integer page,
        @Parameter(description = "Page size, from 1 to 100")
        @RequestParam(required = false) @Min(1) @Max(100) @Nullable Integer size,
        @Parameter(description = "Sort direction by initDate", schema = @Schema(allowableValues = {"asc", "desc"}))
        @RequestParam(defaultValue = "asc") @Pattern(regexp = "asc|desc") String order) {

        PriceHistoryQuery query = new PriceHistoryQuery(from, to,
            currency == null ? null : Currency.getInstance(currency),
            page, size, Sort.Direction.fromString(order));
        return PriceHistoryResponse.from(priceService.getHistory(productId, query));
    }

    @Operation(summary = "Get a price by id")
    @ApiResponse(responseCode = "200", description = "Price found")
    @ApiResponse(responseCode = "404", description = "Product not found, or the price does not belong to it")
    @GetMapping("/{priceId}")
    public PriceResponse getPrice(@PathVariable Long productId, @PathVariable Long priceId) {
        return PriceResponse.from(priceService.getPrice(productId, priceId));
    }

    @Operation(summary = "Replace a price",
        description = "Replaces all the fields of the price. currency is optional and defaults to EUR, "
            + "even if the price had another currency. The price cannot overlap other prices of the product "
            + "in the same currency.")
    @ApiResponse(responseCode = "200", description = "Price updated")
    @ApiResponse(responseCode = "400", description = "Invalid body or currency code, or initDate is not before endDate")
    @ApiResponse(responseCode = "404", description = "Product not found, or the price does not belong to it")
    @ApiResponse(responseCode = "409",
        description = "The price overlaps another price of the product in the same currency")
    @PutMapping("/{priceId}")
    public PriceResponse updatePrice(@PathVariable Long productId, @PathVariable Long priceId,
                                     @Valid @RequestBody PriceRequest request) {
        Price price = priceService.updatePrice(productId, priceId, request.value(), toCurrency(request.currency()),
            request.initDate(), request.endDate());
        return PriceResponse.from(price);
    }

    @Operation(summary = "Delete a price")
    @ApiResponse(responseCode = "204", description = "Price deleted")
    @ApiResponse(responseCode = "404", description = "Product not found, or the price does not belong to it")
    @DeleteMapping("/{priceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePrice(@PathVariable Long productId, @PathVariable Long priceId) {
        priceService.deletePrice(productId, priceId);
    }

    private static Currency toCurrency(@Nullable String code) {
        return code == null ? Price.DEFAULT_CURRENCY : Currency.getInstance(code);
    }
}
