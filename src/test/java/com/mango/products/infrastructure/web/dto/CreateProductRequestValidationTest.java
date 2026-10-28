package com.mango.products.infrastructure.web.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CreateProductRequestValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<String> invalidFields(CreateProductRequest request) {
        Set<ConstraintViolation<CreateProductRequest>> violations = VALIDATOR.validate(request);
        return violations.stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }

    @Test
    void validRequestHasNoErrors() {
        assertThat(invalidFields(new CreateProductRequest("Zapatillas", "Modelo 2025"))).isEmpty();
    }

    // ---- name ----

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void nameCannotBeNullEmptyOrBlank(String name) {
        assertThat(invalidFields(new CreateProductRequest(name, "desc"))).containsExactly("name");
    }

    @Test
    void nameWithOneCharacterIsValid() {
        assertThat(invalidFields(new CreateProductRequest("a", "desc"))).isEmpty();
    }

    @Test
    void nameWithMaxLengthIsValid() {
        assertThat(invalidFields(new CreateProductRequest("a".repeat(255), "desc"))).isEmpty();
    }

    @Test
    void nameOverMaxLengthIsInvalid() {
        assertThat(invalidFields(new CreateProductRequest("a".repeat(256), "desc"))).containsExactly("name");
    }

    // ---- description ----

    @Test
    void descriptionCanBeNull() {
        assertThat(invalidFields(new CreateProductRequest("Zapatillas", null))).isEmpty();
    }

    @Test
    void descriptionCanBeEmpty() {
        assertThat(invalidFields(new CreateProductRequest("Zapatillas", ""))).isEmpty();
    }

    @Test
    void descriptionWithMaxLengthIsValid() {
        assertThat(invalidFields(new CreateProductRequest("Zapatillas", "a".repeat(1000)))).isEmpty();
    }

    @Test
    void descriptionOverMaxLengthIsInvalid() {
        assertThat(invalidFields(new CreateProductRequest("Zapatillas", "a".repeat(1001))))
            .containsExactly("description");
    }
}
