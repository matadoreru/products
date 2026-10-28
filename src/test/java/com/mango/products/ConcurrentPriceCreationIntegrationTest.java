package com.mango.products;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentPriceCreationIntegrationTest extends AbstractIntegrationTest {

    private static final int THREADS = 10;

    @Test
    void onlyOneOfManySimultaneousIdenticalPricesIsAccepted() throws Exception {
        long productId = createProduct("Zapatillas", "Concurrencia");
        String samePrice = """
            {"value": 99.99, "initDate": "2024-01-01", "endDate": "2024-06-30"}
            """;

        List<Integer> statuses = runConcurrently(THREADS, i -> addPrice(productId, samePrice));

        assertThat(statuses).containsOnly(201, 409);
        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(THREADS - 1);
        assertThat(countPrices(productId)).isEqualTo(1);
    }

    @Test
    void onlyOneOfManySimultaneousOverlappingPricesIsAccepted() throws Exception {
        long productId = createProduct("Camiseta", "Concurrencia");

        List<Integer> statuses = runConcurrently(THREADS, i -> addPrice(productId, """
            {"value": %d, "initDate": "2024-01-%02d", "endDate": "2024-01-%02d"}
            """.formatted(10 + i, 1 + i, 15 + i)));

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(THREADS - 1);
        assertThat(countPrices(productId)).isEqualTo(1);
    }

    @Test
    void simultaneousPricesForDifferentProductsAreAllAccepted() throws Exception {
        List<Long> productIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            productIds.add(createProduct("Producto " + i, "Concurrencia"));
        }
        String samePrice = """
            {"value": 99.99, "initDate": "2024-01-01", "endDate": "2024-06-30"}
            """;

        List<Integer> statuses = runConcurrently(THREADS, i -> addPrice(productIds.get(i), samePrice));

        assertThat(statuses).containsOnly(201);
    }

    @FunctionalInterface
    private interface Request {

        int send(int threadIndex) throws Exception;
    }

    private List<Integer> runConcurrently(int threads, Request request) throws Exception {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                int index = i;
                Callable<Integer> task = () -> {
                    ready.countDown();
                    start.await();
                    return request.send(index);
                };
                futures.add(executor.submit(task));
            }
            ready.await();
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        }
    }

    private int addPrice(long productId, String json) {
        return mvc.post().uri("/products/{id}/prices", productId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .exchange()
            .getResponse()
            .getStatus();
    }

    private long createProduct(String name, String description) throws Exception {
        MvcTestResult result = mvc.post().uri("/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "%s", "description": "%s"}
                """.formatted(name, description))
            .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        Number id = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
        return id.longValue();
    }

    private int countPrices(long productId) throws Exception {
        MvcTestResult result = mvc.get().uri("/products/{id}/prices", productId).exchange();
        List<?> prices = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.prices");
        return prices.size();
    }

}
