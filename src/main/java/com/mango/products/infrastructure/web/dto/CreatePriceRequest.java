package com.mango.products.infrastructure.web.dto;

import com.mango.products.infrastructure.web.validation.IsoCurrency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePriceRequest(
    @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal value,
    @NotNull LocalDate initDate,
    @Nullable LocalDate endDate,
    @Nullable @IsoCurrency String currency) {

}
