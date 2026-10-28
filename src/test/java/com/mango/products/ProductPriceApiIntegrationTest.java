package com.mango.products;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPriceApiIntegrationTest extends AbstractIntegrationTest {

    // ------------------------------------------------------------------
    // Escenario completo del benchmark.sh
    // ------------------------------------------------------------------

    @Test
    void benchmarkScenario() throws Exception {
        long id = createProduct("Zapatillas deportivas", "Modelo 2025 edicion limitada");

        assertThat(addPrice(id, "99.99", "2024-01-01", "2024-06-30")).hasStatus(HttpStatus.CREATED);
        assertThat(addPrice(id, "129.99", "2024-07-01", "2024-12-31")).hasStatus(HttpStatus.CREATED);
        assertThat(addPrice(id, "199.99", "2025-01-01", null)).hasStatus(HttpStatus.CREATED);

        assertThat(priceAt(id, "2024-04-15")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
            {"value": 99.99}
            """);
        assertThat(priceAt(id, "2024-08-15")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
            {"value": 129.99}
            """);
        assertThat(priceAt(id, "2025-03-01")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
            {"value": 199.99}
            """);

        assertThat(history(id)).hasStatusOk().bodyJson().isStrictlyEqualTo("""
            {
              "name": "Zapatillas deportivas",
              "description": "Modelo 2025 edicion limitada",
              "prices": [
                {"value": 99.99,  "initDate": "2024-01-01", "endDate": "2024-06-30"},
                {"value": 129.99, "initDate": "2024-07-01", "endDate": "2024-12-31"},
                {"value": 199.99, "initDate": "2025-01-01", "endDate": null}
              ]
            }
            """);
    }

    // ------------------------------------------------------------------
    // Productos
    // ------------------------------------------------------------------

    @Test
    void createProductReturnsLocationAndIdAsFirstField() throws Exception {
        MvcTestResult result = postJson("/products", """
            {"name": "Camiseta", "description": "Algodon"}
            """);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).headers().containsHeader(HttpHeaders.LOCATION);

        // benchmark.sh extrae el id con grep: debe ser el primer campo del JSON
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).startsWith("{\"id\":");
    }

    @Test
    void createProductWithoutNameReturns400() {
        assertThat(postJson("/products", """
            {"description": "sin nombre"}
            """))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson().extractingPath("$.errors.name").isNotNull();
    }

    @Test
    void getExistingProduct() throws Exception {
        long id = createProduct("Pantalon", "Vaquero");

        assertThat(mvc.get().uri("/products/{id}", id))
            .hasStatusOk()
            .bodyJson().extractingPath("$.name").isEqualTo("Pantalon");
    }

    // ------------------------------------------------------------------
    // Errores de precios
    // ------------------------------------------------------------------

    @Test
    void overlappingPriceReturns409() throws Exception {
        long id = createProduct("Chaqueta", "Invierno");
        addPrice(id, "99.99", "2024-01-01", "2024-06-30");

        assertThat(addPrice(id, "50", "2024-03-01", "2024-08-31")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void priceSharingLastDayReturns409() throws Exception {
        long id = createProduct("Bufanda", "Lana");
        addPrice(id, "99.99", "2024-01-01", "2024-06-30");

        assertThat(addPrice(id, "50", "2024-06-30", "2024-07-15")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void openEndedPriceBlocksAnyLaterPrice() throws Exception {
        long id = createProduct("Gorro", "Punto");
        addPrice(id, "10", "2024-01-01", null);

        assertThat(addPrice(id, "20", "2030-01-01", "2030-12-31")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void consecutivePricesAreAccepted() throws Exception {
        long id = createProduct("Calcetines", "Pack 3");

        assertThat(addPrice(id, "5", "2024-01-01", "2024-01-31")).hasStatus(HttpStatus.CREATED);
        assertThat(addPrice(id, "6", "2024-02-01", "2024-02-29")).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void initDateAfterEndDateReturns400() throws Exception {
        long id = createProduct("Cinturon", "Piel");

        assertThat(addPrice(id, "50", "2030-06-01", "2030-01-01")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidDateFormatReturns400() throws Exception {
        long id = createProduct("Guantes", "Cuero");

        assertThat(mvc.get().uri("/products/{id}/prices", id).param("date", "15-04-2024"))
            .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void dateWithoutPriceReturns404() throws Exception {
        long id = createProduct("Sudadera", "Capucha");
        addPrice(id, "40", "2024-01-01", "2024-06-30");

        assertThat(priceAt(id, "2020-01-01"))
            .hasStatus(HttpStatus.NOT_FOUND)
            .bodyJson().extractingPath("$.title").isEqualTo("Price not found");
    }

    @Test
    void gapBetweenPricesReturns404() throws Exception {
        long id = createProduct("Abrigo", "Largo");
        addPrice(id, "100", "2024-01-01", "2024-01-31");
        addPrice(id, "110", "2024-03-01", "2024-03-31");

        assertThat(priceAt(id, "2024-02-15")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void unknownProductReturns404OnEveryEndpoint() {
        long unknown = 999_999L;

        assertThat(history(unknown))
            .hasStatus(HttpStatus.NOT_FOUND)
            .bodyJson().extractingPath("$.title").isEqualTo("Product not found");
        assertThat(priceAt(unknown, "2024-04-15"))
            .hasStatus(HttpStatus.NOT_FOUND)
            .bodyJson().extractingPath("$.title").isEqualTo("Product not found");
        assertThat(addPrice(unknown, "10", "2024-01-01", null))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void historyOfProductWithoutPricesIsEmpty() throws Exception {
        long id = createProduct("Polo", "Manga corta");

        assertThat(history(id))
            .hasStatusOk()
            .bodyJson().extractingPath("$.prices").asArray().isEmpty();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private long createProduct(String name, String description) throws Exception {
        MvcTestResult result = postJson("/products", """
            {"name": "%s", "description": "%s"}
            """.formatted(name, description));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        Number id = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
        return id.longValue();
    }

    private MvcTestResult addPrice(long productId, String value, String initDate, String endDate) {
        String end = endDate == null ? "null" : "\"" + endDate + "\"";
        return postJson("/products/" + productId + "/prices", """
            {"value": %s, "initDate": "%s", "endDate": %s}
            """.formatted(value, initDate, end));
    }

    private MvcTestResult priceAt(long productId, String date) {
        return mvc.get().uri("/products/{id}/prices", productId).param("date", date).exchange();
    }

    private MvcTestResult history(long productId) {
        return mvc.get().uri("/products/{id}/prices", productId).exchange();
    }

    private MvcTestResult postJson(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    // OpenAPI
    @Test
    void openApiDocumentIsPublished() {
        assertThat(mvc.get().uri("/v3/api-docs"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.paths").asMap()
            .containsKeys("/products", "/products/{id}", "/products/{productId}/prices");
    }
}
