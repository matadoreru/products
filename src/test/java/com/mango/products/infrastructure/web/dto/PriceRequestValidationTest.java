package com.mango.products.infrastructure.web.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PriceRequestValidationTest {

    private static final LocalDate INIT = LocalDate.parse("2024-01-01");
    private static final LocalDate END = LocalDate.parse("2024-06-30");

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<String> invalidFields(PriceRequest request) {
        Set<ConstraintViolation<PriceRequest>> violations = VALIDATOR.validate(request);
        return violations.stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }

    private static PriceRequest withValue(String value) {
        return new PriceRequest(new BigDecimal(value), INIT, END, null);
    }

    @Test
    void validRequestHasNoErrors() {
        assertThat(invalidFields(withValue("99.99"))).isEmpty();
    }

    // ---- value ----

    @Test
    void valueCannotBeNull() {
        assertThat(invalidFields(new PriceRequest(null, INIT, END, null))).containsExactly("value");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0.01",           // mínimo positivo con 2 decimales
        "1",              // sin decimales
        "1.5",            // un decimal
        "9999999999.99"   // máximo: 10 enteros y 2 decimales
    })
    void valueWithinLimitsIsValid(String value) {
        assertThat(invalidFields(withValue(value))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0",              // cero no es positivo
        "0.00",
        "-0.01",          // negativo
        "-100"
    })
    void valueMustBePositive(String value) {
        assertThat(invalidFields(withValue(value))).containsExactly("value");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "10000000000",    // 11 enteros, uno más del máximo
        "1.001",          // 3 decimales
        "0.001"           // positivo pero con 3 decimales
    })
    void valueOutOfPrecisionIsInvalid(String value) {
        assertThat(invalidFields(withValue(value))).containsExactly("value");
    }

    // ---- fechas ----

    @Test
    void initDateCannotBeNull() {
        assertThat(invalidFields(new PriceRequest(new BigDecimal("10"), null, END, null))).containsExactly("initDate");
    }

    @Test
    void endDateCanBeNull() {
        assertThat(invalidFields(new PriceRequest(new BigDecimal("10"), INIT, null, null))).isEmpty();
    }

    @Test
    void severalErrorsAreReportedTogether() {
        assertThat(invalidFields(new PriceRequest(null, null, null, null)))
            .containsExactlyInAnyOrder("value", "initDate");
    }

    @ParameterizedTest
    @ValueSource(strings = {"EUR", "USD", "GBP", "JPY"})
    void validCurrencyCodesAreAccepted(String currency) {
        assertThat(invalidFields(new PriceRequest(new BigDecimal("10"), INIT, END, currency))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"eur", "ABC", "EURO", "E", ""})
    void invalidCurrencyCodesAreRejected(String currency) {
        assertThat(invalidFields(new PriceRequest(new BigDecimal("10"), INIT, END, currency)))
            .containsExactly("currency");
    }

    @Test
    void currencyCanBeNull() {
        assertThat(invalidFields(new PriceRequest(new BigDecimal("10"), INIT, END, null))).isEmpty();
    }
}
