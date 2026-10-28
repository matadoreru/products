package com.mango.products.domain.model;

import com.mango.products.domain.exception.InvalidPriceRangeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PriceTest {

    private static final Product PRODUCT = new Product("Zapatillas", "Modelo 2025");
    private static final BigDecimal VALUE = new BigDecimal("99.99");

    private static Price price(String initDate, String endDate) {
        return new Price(PRODUCT, VALUE, LocalDate.parse(initDate), endDate == null ? null : LocalDate.parse(endDate), Price.DEFAULT_CURRENCY);
    }

    @Nested
    class Constructor {

        @Test
        void acceptsInitDateBeforeEndDate() {
            assertThatCode(() -> price("2024-01-01", "2024-06-30")).doesNotThrowAnyException();
        }

        @Test
        void acceptsNullEndDateAsOpenEnded() {
            Price price = price("2024-01-01", null);
            assertThat(price.getEndDate()).isNull();
        }

        @Test
        void acceptsRangeOfTwoDays() {
            assertThatCode(() -> price("2024-01-01", "2024-01-02")).doesNotThrowAnyException();
        }

        @Test
        void rejectsInitDateEqualToEndDate() {
            assertThatThrownBy(() -> price("2024-01-01", "2024-01-01"))
                .isInstanceOf(InvalidPriceRangeException.class);
        }

        @Test
        void rejectsInitDateAfterEndDate() {
            assertThatThrownBy(() -> price("2024-06-30", "2024-01-01"))
                .isInstanceOf(InvalidPriceRangeException.class);
        }
    }

    @Nested
    class IsActive {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "2023-12-31, false",  // día anterior al inicio
            "2024-01-01, true",   // primer día, incluido
            "2024-03-15, true",   // en medio
            "2024-06-30, true",   // último día, incluido
            "2024-07-01, false"   // día posterior al fin
        })
        void closedRange(LocalDate date, boolean expected) {
            assertThat(price("2024-01-01", "2024-06-30").isActive(date)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "2024-12-31, false",  // día anterior al inicio
            "2025-01-01, true",   // primer día
            "2099-12-31, true"    // muy en el futuro: no tiene fin
        })
        void openEndedRange(LocalDate date, boolean expected) {
            assertThat(price("2025-01-01", null).isActive(date)).isEqualTo(expected);
        }
    }

    @Nested
    class Overlaps {

        // Precio existente: del 2024-01-01 al 2024-06-30
        @ParameterizedTest(name = "[{0}, {1}] -> {2}")
        @CsvSource({
            "2024-07-01, 2024-12-31, false",  // empieza justo después
            "2023-01-01, 2023-12-31, false",  // termina justo antes
            "2023-01-01, 2024-01-01, true",   // comparte el primer día
            "2024-06-30, 2024-12-31, true",   // comparte el último día
            "2024-03-01, 2024-08-31, true",   // solape parcial por el final
            "2023-06-01, 2024-02-01, true",   // solape parcial por el inicio
            "2024-02-01, 2024-03-01, true",   // contenido dentro
            "2023-01-01, 2025-01-01, true",   // lo contiene entero
            "2024-01-01, 2024-06-30, true",   // exactamente el mismo rango
            "2024-07-01,           , false",  // sin fin, empieza después
            "2024-06-30,           , true",   // sin fin, empieza el último día
            "2023-01-01,           , true"    // sin fin, empieza antes
        })
        void againstClosedRange(LocalDate otherInit, LocalDate otherEnd, boolean expected) {
            Price existing = price("2024-01-01", "2024-06-30");
            assertThat(existing.overlaps(otherInit, otherEnd)).isEqualTo(expected);
        }

        // Precio existente: desde el 2025-01-01, sin fin
        @ParameterizedTest(name = "[{0}, {1}] -> {2}")
        @CsvSource({
            "2024-01-01, 2024-12-31, false",  // termina el día antes
            "2024-01-01, 2025-01-01, true",   // comparte el primer día
            "2030-01-01, 2030-12-31, true",   // cualquier rango futuro
            "2030-01-01,           , true"    // dos rangos sin fin siempre se solapan
        })
        void againstOpenEndedRange(LocalDate otherInit, LocalDate otherEnd, boolean expected) {
            Price existing = price("2025-01-01", null);
            assertThat(existing.overlaps(otherInit, otherEnd)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "[{0}, {1}] vs [{2}, {3}]")
        @CsvSource({
            "2024-01-01, 2024-06-30, 2024-07-01, 2024-12-31",
            "2024-01-01, 2024-06-30, 2024-06-30, 2024-12-31",
            "2024-01-01,           , 2023-01-01, 2023-12-31",
            "2024-01-01,           , 2030-01-01,           "
        })
        void isSymmetric(LocalDate aInit, LocalDate aEnd, LocalDate bInit, LocalDate bEnd) {
            Price a = new Price(PRODUCT, VALUE, aInit, aEnd, Price.DEFAULT_CURRENCY);
            Price b = new Price(PRODUCT, VALUE, bInit, bEnd, Price.DEFAULT_CURRENCY);
            assertThat(a.overlaps(bInit, bEnd)).isEqualTo(b.overlaps(aInit, aEnd));
        }
    }

}
