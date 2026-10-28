package com.mango.products;

import com.jayway.jsonpath.JsonPath;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared PostgreSQL container and request helpers for the HTTP integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvcTester mvc;

    protected long createProduct(String name, String description) throws Exception {
        MvcTestResult result = postJson("/products", """
            {"name": "%s", "description": "%s"}
            """.formatted(name, description));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        return idOf(result);
    }

    protected MvcTestResult addPrice(long productId, String value, String initDate, @Nullable String endDate) {
        return addPriceIn(productId, value, null, initDate, endDate);
    }

    protected MvcTestResult addPriceIn(long productId, String value, @Nullable String currency,
                                       String initDate, @Nullable String endDate) {
        return postJson("/products/" + productId + "/prices", """
            {"value": %s, "currency": %s, "initDate": "%s", "endDate": %s}
            """.formatted(value, jsonString(currency), initDate, jsonString(endDate)));
    }

    protected MvcTestResult priceAt(long productId, String date) {
        return mvc.get().uri("/products/{id}/prices", productId).param("date", date).exchange();
    }

    protected MvcTestResult history(long productId) {
        return mvc.get().uri("/products/{id}/prices", productId).exchange();
    }

    protected MvcTestResult postJson(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    protected static long idOf(MvcTestResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
        return id.longValue();
    }

    private static String jsonString(@Nullable String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
