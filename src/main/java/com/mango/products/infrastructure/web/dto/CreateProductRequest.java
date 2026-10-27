package com.mango.products.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
    @NotBlank @Size(max = 255)
    String name,

    @Size(max = 1000)
    String description
) {

}
