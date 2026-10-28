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
            {"value": 99.99, "currency": "EUR"}
            """);
        assertThat(priceAt(id, "2024-08-15")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
            {"value": 129.99, "currency": "EUR"}
            """);
        assertThat(priceAt(id, "2025-03-01")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
            {"value": 199.99, "currency": "EUR"}
            """);

        assertThat(history(id)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
            {
              "name": "Zapatillas deportivas",
              "description": "Modelo 2025 edicion limitada",
              "prices": [
                {"value": 99.99,  "currency": "EUR", "initDate": "2024-01-01", "endDate": "2024-06-30"},
                {"value": 129.99, "currency": "EUR", "initDate": "2024-07-01", "endDate": "2024-12-31"},
                {"value": 199.99, "currency": "EUR", "initDate": "2025-01-01", "endDate": null}
              ]
            }
            """);
        assertThat(history(id)).bodyJson().extractingPath("$.prices[*].id").asArray()
            .hasSize(3).doesNotContainNull();
        assertThat(history(id)).bodyJson().doesNotHavePath("$.page");
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

    private MvcTestResult addPriceIn(long productId, String value, String currency,
                                     String initDate, String endDate) {
        String end = endDate == null ? "null" : "\"" + endDate + "\"";
        return postJson("/products/" + productId + "/prices", """
            {"value": %s, "currency": "%s", "initDate": "%s", "endDate": %s}
            """.formatted(value, currency, initDate, end));
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
        MvcTestResult result = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(result)
            .hasStatusOk()
            .bodyJson().extractingPath("$.paths").asMap()
            .containsKeys("/products", "/products/{id}", "/products/{productId}/prices");

        assertThat(result)
            .bodyJson()
            .extractingPath("$.paths['/products/{productId}/prices'].get.parameters[?(@.name == 'date')].required")
            .asArray()
            .containsExactly(false);
    }

    // Page
    @Test
    void historyCanBePaginated() throws Exception {
        long id = createProductWithThreePrices();

        assertThat(mvc.get().uri("/products/{id}/prices", id).param("page", "0").param("size", "2"))
            .hasStatusOk()
            .bodyJson().isLenientlyEqualTo("""
                {
                  "prices": [
                    {"value": 99.99,  "initDate": "2024-01-01"},
                    {"value": 129.99, "initDate": "2024-07-01"}
                  ],
                  "page": {"number": 0, "size": 2, "totalElements": 3, "totalPages": 2}
                }
                """);

        assertThat(mvc.get().uri("/products/{id}/prices", id).param("page", "1").param("size", "2"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.prices[*].value").asArray().containsExactly(199.99);
    }

    @Test
    void historyCanBeSortedDescending() throws Exception {
        long id = createProductWithThreePrices();

        assertThat(mvc.get().uri("/products/{id}/prices", id).param("order", "desc"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.prices[*].value").asArray()
            .containsExactly(199.99, 129.99, 99.99);
    }

    @Test
    void historyWithoutPaginationHasNoPageBlock() throws Exception {
        long id = createProductWithThreePrices();

        assertThat(mvc.get().uri("/products/{id}/prices", id))
            .hasStatusOk()
            .bodyJson().doesNotHavePath("$.page");
    }

    @Test
    void invalidPaginationParametersReturn400() throws Exception {
        long id = createProductWithThreePrices();

        assertThat(mvc.get().uri("/products/{id}/prices", id).param("page", "-1"))
            .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri("/products/{id}/prices", id).param("size", "0"))
            .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri("/products/{id}/prices", id).param("size", "101"))
            .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri("/products/{id}/prices", id).param("order", "random"))
            .hasStatus(HttpStatus.BAD_REQUEST);
    }

    private long createProductWithThreePrices() throws Exception {
        long id = createProduct("Paginado", "Tres precios");
        addPrice(id, "99.99", "2024-01-01", "2024-06-30");
        addPrice(id, "129.99", "2024-07-01", "2024-12-31");
        addPrice(id, "199.99", "2025-01-01", null);
        return id;
    }

    @Test
    void historyCanBeFilteredByDateRange() throws Exception {
        long id = createProductWithThreePrices();

        assertThat(mvc.get().uri("/products/{id}/prices", id)
            .param("from", "2024-06-01").param("to", "2024-08-31"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.prices[*].value").asArray()
            .containsExactly(99.99, 129.99);
    }

    @Test
    void historyCanBeFilteredWithOnlyOneBound() throws Exception {
        long id = createProductWithThreePrices();

        assertThat(mvc.get().uri("/products/{id}/prices", id).param("from", "2025-02-01"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.prices[*].value").asArray()
            .containsExactly(199.99);

        assertThat(mvc.get().uri("/products/{id}/prices", id).param("to", "2024-03-01"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.prices[*].value").asArray()
            .containsExactly(99.99);
    }

    @Test
    void filterCombinesWithPagination() throws Exception {
        long id = createProductWithThreePrices();

        assertThat(mvc.get().uri("/products/{id}/prices", id)
            .param("from", "2024-07-01").param("size", "1"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.page.totalElements").isEqualTo(2);
    }

    @Test
    void filterWithFromAfterToReturns400() throws Exception {
        long id = createProductWithThreePrices();

        assertThat(mvc.get().uri("/products/{id}/prices", id)
            .param("from", "2025-01-01").param("to", "2024-01-01"))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson().extractingPath("$.title").isEqualTo("Invalid filter");
    }

    // Currency
    @Test
    void samePeriodInDifferentCurrenciesIsAllowed() throws Exception {
        long id = createProduct("Multimoneda", "EUR y USD");

        assertThat(addPrice(id, "99.99", "2024-01-01", "2024-06-30")).hasStatus(HttpStatus.CREATED);
        assertThat(addPriceIn(id, "109.99", "USD", "2024-01-01", "2024-06-30")).hasStatus(HttpStatus.CREATED);

        assertThat(priceAt(id, "2024-04-15")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
            {"value": 99.99, "currency": "EUR"}
            """);
        assertThat(mvc.get().uri("/products/{id}/prices", id)
            .param("date", "2024-04-15").param("currency", "USD"))
            .hasStatusOk().bodyJson().isStrictlyEqualTo("""
                {"value": 109.99, "currency": "USD"}
                """);
    }

    @Test
    void overlappingPriceInSameCurrencyReturns409() throws Exception {
        long id = createProduct("Solape USD", "Misma moneda");
        addPriceIn(id, "10", "USD", "2024-01-01", "2024-06-30");

        assertThat(addPriceIn(id, "20", "USD", "2024-03-01", "2024-08-31")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void priceAtDateInCurrencyWithoutPricesReturns404() throws Exception {
        long id = createProduct("Solo EUR", "Sin precios en USD");
        addPrice(id, "10", "2024-01-01", null);

        assertThat(mvc.get().uri("/products/{id}/prices", id)
            .param("date", "2024-04-15").param("currency", "USD"))
            .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidCurrencyReturns400() throws Exception {
        long id = createProduct("Moneda invalida", "Codigos incorrectos");

        assertThat(addPriceIn(id, "10", "ABC", "2024-01-01", null))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson().extractingPath("$.errors.currency").isNotNull();
        assertThat(mvc.get().uri("/products/{id}/prices", id)
            .param("date", "2024-04-15").param("currency", "usd"))
            .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void historyCanBeFilteredByCurrency() throws Exception {
        long id = createProduct("Filtro moneda", "EUR y USD");
        addPrice(id, "99.99", "2024-01-01", null);
        addPriceIn(id, "109.99", "USD", "2024-01-01", null);

        assertThat(mvc.get().uri("/products/{id}/prices", id).param("currency", "USD"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.prices[*].currency").asArray().containsExactly("USD");
        assertThat(history(id))
            .hasStatusOk()
            .bodyJson().extractingPath("$.prices").asArray().hasSize(2);
    }

    private long priceIdOf(MvcTestResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
        return id.longValue();
    }

    private MvcTestResult updatePrice(long productId, long priceId, String json) {
        return mvc.put().uri("/products/{productId}/prices/{priceId}", productId, priceId)
            .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult deletePrice(long productId, long priceId) {
        return mvc.delete().uri("/products/{productId}/prices/{priceId}", productId, priceId).exchange();
    }

    @Test
    void createdPriceHasIdAndLocation() throws Exception {
        long id = createProduct("Con Location", "Precio nuevo");
        MvcTestResult result = addPrice(id, "10", "2024-01-01", null);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        long priceId = priceIdOf(result);
        assertThat(result).headers().hasValue(HttpHeaders.LOCATION,
            "http://localhost/products/" + id + "/prices/" + priceId);
        assertThat(mvc.get().uri("/products/{id}/prices/{priceId}", id, priceId))
            .hasStatusOk().bodyJson().extractingPath("$.value").isEqualTo(10.0);
    }

    @Test
    void priceOfAnotherProductReturns404() throws Exception {
        long owner = createProduct("Propietario", "Tiene el precio");
        long other = createProduct("Otro", "No tiene el precio");
        long priceId = priceIdOf(addPrice(owner, "10", "2024-01-01", null));

        assertThat(mvc.get().uri("/products/{id}/prices/{priceId}", other, priceId))
            .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(deletePrice(other, priceId)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void priceCanBeUpdated() throws Exception {
        long id = createProduct("Actualizable", "Cambia de precio");
        long priceId = priceIdOf(addPrice(id, "10", "2024-01-01", "2024-06-30"));

        assertThat(updatePrice(id, priceId, """
            {"value": 15, "initDate": "2024-02-01", "endDate": "2024-07-31"}
            """)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
            {"value": 15, "currency": "EUR", "initDate": "2024-02-01", "endDate": "2024-07-31"}
            """);

        assertThat(priceAt(id, "2024-07-15")).hasStatusOk()
            .bodyJson().extractingPath("$.value").isEqualTo(15.0);
        assertThat(priceAt(id, "2024-01-15")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateKeepingTheSamePeriodIsNotAnOverlap() throws Exception {
        long id = createProduct("Mismo periodo", "Solo cambia el importe");
        long priceId = priceIdOf(addPrice(id, "10", "2024-01-01", "2024-06-30"));

        assertThat(updatePrice(id, priceId, """
            {"value": 12, "initDate": "2024-01-01", "endDate": "2024-06-30"}
            """)).hasStatusOk();
    }

    @Test
    void updateOverlappingAnotherPriceReturns409() throws Exception {
        long id = createProduct("Solape al actualizar", "Dos precios");
        addPrice(id, "10", "2024-01-01", "2024-06-30");
        long secondId = priceIdOf(addPrice(id, "20", "2024-07-01", "2024-12-31"));

        assertThat(updatePrice(id, secondId, """
            {"value": 20, "initDate": "2024-06-01", "endDate": "2024-12-31"}
            """)).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void updateWithInvalidDataReturns400() throws Exception {
        long id = createProduct("Datos invalidos", "Al actualizar");
        long priceId = priceIdOf(addPrice(id, "10", "2024-01-01", null));

        assertThat(updatePrice(id, priceId, """
            {"value": 10, "initDate": "2024-06-30", "endDate": "2024-01-01"}
            """)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(updatePrice(id, priceId, """
            {"value": -5, "initDate": "2024-01-01", "endDate": null}
            """)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateUnknownPriceReturns404() throws Exception {
        long id = createProduct("Sin ese precio", "Id inexistente");

        assertThat(updatePrice(id, 999_999L, """
            {"value": 10, "initDate": "2024-01-01", "endDate": null}
            """)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletedPriceFreesItsPeriod() throws Exception {
        long id = createProduct("Borrable", "Libera su periodo");
        long priceId = priceIdOf(addPrice(id, "10", "2024-01-01", "2024-06-30"));

        assertThat(deletePrice(id, priceId)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/products/{id}/prices/{priceId}", id, priceId))
            .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(addPrice(id, "11", "2024-03-01", "2024-08-31")).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void deleteUnknownPriceReturns404() throws Exception {
        long id = createProduct("Nada que borrar", "Id inexistente");

        assertThat(deletePrice(id, 999_999L)).hasStatus(HttpStatus.NOT_FOUND);
    }
}
