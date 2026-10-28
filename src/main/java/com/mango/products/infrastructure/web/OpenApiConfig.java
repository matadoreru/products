package com.mango.products.infrastructure.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productsOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Products API")
            .version("1.0.0")
            .description("Product catalogue with historical prices."));
    }

    @Bean
    public OpenApiCustomizer optionalDateOnPricesEndpoint() {
        return openApi -> {
            PathItem prices = openApi.getPaths().get("/products/{productId}/prices");
            if (prices == null || prices.getGet() == null || prices.getGet().getParameters() == null) {
                return;
            }
            prices.getGet().getParameters().stream()
                .filter(parameter -> "date".equals(parameter.getName()))
                .forEach(parameter -> parameter.setRequired(false));
        };
    }
}
