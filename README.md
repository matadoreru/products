# Productos con precios históricos (Capitole Hortensi Martin Florit)

Solución a la prueba técnica de backend de Mango (Capitole) de Hortensi Martin Florit.
API REST para gestionar productos y su historial de precios. Cada producto puede tener varios precios a lo largo del
tiempo, pero solo uno vigente para una misma fecha y moneda.

- **Stack**: Java 25, Spring Boot 4.1, Spring Data JPA, PostgreSQL 17, Flyway.
- **Rendimiento**: arranque en unos 4 s gracias a la caché AOT de Java 25; con la JVM caliente,
  más de 2 400 peticiones/s por endpoint con p95 < 70 ms, con la aplicación limitada a 1 CPU y 1 GB.
- **Calidad**: 115 tests (unitarios, de validación, de integración contra PostgreSQL real y de concurrencia), OpenAPI
  con Swagger UI, datos de ejemplo opcionales.
- **Todos los opcionales del enunciado** implementados salvo la autenticación, descartada porque el `benchmark.sh` del
  enunciado no envía credenciales.

## Inicio rápido

Requisitos: Java 25 y Docker.

```bash
# Tests (levanta PostgreSQL con Testcontainers)
./gradlew test

# Aplicación + benchmark del enunciado, todo en contenedores
docker compose up --build --exit-code-from benchmark

# Prueba de carga con k6
docker compose up -d --build db app
docker compose --profile k6 run --rm k6

# Limpiar contenedores y volúmenes
docker compose down -v
```

Para ejecutar la aplicación fuera de Docker hace falta un PostgreSQL en `localhost:5432` con usuario, contraseña y base
de datos `products`:

```bash
docker run -d --name products-db -p 5432:5432 \
  -e POSTGRES_DB=products -e POSTGRES_USER=products -e POSTGRES_PASSWORD=products \
  postgres:17-alpine

./gradlew bootRun

# Con datos de ejemplo (perfil dev)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

La conexión se configura con las variables `DB_URL`, `DB_USER` y `DB_PASSWORD`. Con la aplicación levantada:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs
- Salud: http://localhost:8080/actuator/health

## Stack y justificación

| Tecnología                       | Motivo                                                                                                                                                                                       |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Java 25 (LTS)                    | Arranque y consumo de memoria mejores que Java 21, y caché AOT (JEP 483/514/515), que es la mayor mejora de arranque del proyecto. Además, uso Fedora 44 como SO y ya no distribuye Java 21. |
| Gradle 9.2.1 (wrapper)           | Gradle 8.14 no se ejecuta sobre Java 25.                                                                                                                                                     |
| Spring Boot 4.1.2 con Spring MVC | La rama 3.5 dejó de tener soporte libre en junio de 2026. Spring MVC con hilos virtuales cubre la concurrencia necesaria sin pasar a reactivo.                                               |
| Spring Data JPA + Hibernate 7    | Estándar en proyectos Spring.                                                                                                                                                                |
| PostgreSQL 17                    | Constraint de exclusión sobre rangos de fechas (`btree_gist`): la base de datos garantiza el no solape aunque fallase el código.                                                             |
| Flyway                           | Esquema versionado y datos de ejemplo.                                                                                                                                                       |
| Hilos virtuales                  | Muchas conexiones simultáneas con 1 CPU sin ajustar pools de hilos.                                                                                                                          |
| Bean Validation + `@IsoCurrency` | Validación declarativa de la entrada, con una anotación propia para códigos ISO 4217.                                                                                                        |
| springdoc-openapi 3.1.0          | Swagger UI y documento OpenAPI.                                                                                                                                                              |
| JSpecify (`@NullMarked`)         | Anotaciones de nulabilidad que usa el propio Spring 7.                                                                                                                                       |
| Testcontainers 2 + MockMvcTester | Tests de integración contra un PostgreSQL real, no contra H2.                                                                                                                                |
| k6 1.0.0                         | Prueba de carga a la API.                                                                                                                                                                    |

## Arquitectura

Paquete raíz `com.mango.products`, organizado por capas:

| Paquete                      | Contenido                                                                                                                |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `domain.model`               | Entidades `Product` y `Price`. `Price` valida su propio rango de fechas y expone `isActive`, `overlaps` y `update`.      |
| `domain.exception`           | Excepciones de negocio (producto o precio no encontrado, solape, rango inválido, filtro inválido).                       |
| `application`                | `ProductService`, `PriceService`, `PriceHistoryQuery` (filtros, paginación y orden), `ProductPriceHistory` y `PageInfo`. |
| `infrastructure.persistence` | Repositorios Spring Data y `PriceSpecifications` (filtros del historial).                                                |
| `infrastructure.web`         | Controladores, DTO (records), `ApiExceptionHandler`, `OpenApiConfig` y `validation` (`@IsoCurrency`).                    |

## Modelo de datos

Dos tablas, gestionadas por Flyway:

| Migración                     | Contenido                                                                                                                                                                                                                                            |
|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `V1__init.sql`                | Tablas `product` y `price`. `value NUMERIC(12,2)` (`BigDecimal` en Java). CHECK `price_valid_range` (`init_date < end_date`). Constraint de exclusión `price_no_overlap` con `btree_gist` sobre `daterange(init_date, end_date, '[]')`.              |
| `V2__add_currency.sql`        | Columna `currency VARCHAR(3) NOT NULL DEFAULT 'EUR'` con CHECK `^[A-Z]{3}$`. La exclusión pasa a ser por producto **y** moneda. Índice `(product_id, currency, init_date)`, que respalda la consulta del precio vigente y el historial por producto. |
| `db/seed/V3__sample_data.sql` | Datos de ejemplo, solo con el perfil `dev` (`application-dev.yml` añade `classpath:db/seed` a las ubicaciones de Flyway).                                                                                                                            |

Los datos de ejemplo contienen seis productos que cubren los casos interesantes.

Limitación conocida: la semilla es una migración versionada, así que arrancar con y sin perfil `dev` sobre la misma base
de datos puede descuadrar el historial de Flyway.

Para desarrollo local basta con recrear la base de datos.

## Supuestos

- `endDate` es **inclusiva**. El `benchmark.sh` del enunciado añade precios consecutivos (`2024-06-30` y `2024-07-01`)
  que deben convivir.
- `endDate: null` significa precio sin fecha de fin.
- `initDate` debe ser **estrictamente anterior** a `endDate` cuando ambas existen.
- Un precio sin moneda es en **EUR**.

## API

Las rutas del enunciado se mantienen intactas. Los endpoints del enunciado se ampliaron con campos adicionales y se
añadieron otros; el `benchmark.sh` original funciona sin cambios.

| Método y ruta                                      | Descripción                                                                                                          |
|----------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `POST /products`                                   | Crea un producto. 201 con cabecera `Location`. `id` es el primer campo del JSON (el benchmark lo extrae con `grep`). |
| `GET /products/{id}`                               | Devuelve un producto. Añadido para que `Location` apunte a un recurso real.                                          |
| `POST /products/{productId}/prices`                | Añade un precio. `currency` opcional (EUR por defecto). 201 con `Location`.                                          |
| `GET /products/{productId}/prices?date=&currency=` | Precio vigente en una fecha. Devuelve `{value, currency}`. `currency` por defecto EUR.                               |
| `GET /products/{productId}/prices`                 | Historial de precios con filtros, paginación y orden opcionales (ver abajo).                                         |
| `GET /products/{productId}/prices/{priceId}`       | Devuelve un precio. Solo accesible desde su producto.                                                                |
| `PUT /products/{productId}/prices/{priceId}`       | Reemplazo completo del precio. Sin `currency` pasa a EUR. No hay PATCH.                                              |
| `DELETE /products/{productId}/prices/{priceId}`    | Borra un precio. 204.                                                                                                |

Parámetros del historial, todos opcionales:

| Parámetro                                      | Significado                                                                                     |
|------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `from`, `to`                                   | Solo precios vigentes en algún momento del rango.                                               |
| `currency`                                     | Solo precios en esa moneda.                                                                     |
| `page` (≥ 0), `size` (1 a 100, 20 por defecto) | Paginación. Si no se indica ninguno se devuelve todo, sin bloque `page` y sin consulta `COUNT`. |
| `order` (`asc` o `desc`)                       | Orden por `initDate`, ascendente por defecto.                                                   |

Ejemplos:

```bash
curl -X POST localhost:8080/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Zapatillas deportivas","description":"Modelo 2025 edición limitada"}'

curl -X POST localhost:8080/products/1/prices \
  -H 'Content-Type: application/json' \
  -d '{"value":99.99,"initDate":"2024-01-01","endDate":"2024-06-30"}'

curl 'localhost:8080/products/1/prices?date=2024-04-15'
# {"value":99.99,"currency":"EUR"}

curl 'localhost:8080/products/1/prices?from=2024-06-01&size=10&order=desc'
```

Códigos de respuesta:

| Código          | Cuándo                                                                                                                                                |
|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| 200 / 201 / 204 | Lectura / creación / borrado.                                                                                                                         |
| 400             | Cuerpo inválido, formato de fecha o moneda incorrecto, `initDate` no anterior a `endDate`, filtro `from` posterior a `to`, paginación fuera de rango. |
| 404             | Producto inexistente, precio inexistente o de otro producto, o sin precio vigente en esa fecha y moneda.                                              |
| 409             | El precio solapa otro del mismo producto y moneda.                                                                                                    |

Los errores siguen el formato:

```json
{
  "detail": "Validation failed",
  "instance": "/products/1/prices",
  "status": 400,
  "title": "Invalid request",
  "errors": {
    "value": "must be greater than 0",
    "initDate": "must not be null"
  }
}
```

## Concurrencia

Los tests de concurrencia (diez peticiones simultáneas del mismo precio para el mismo producto) sacaron a la luz
deadlocks en PostgreSQL (SQLSTATE `40P01`).

La solución es bloquear la fila del producto (`SELECT ... FOR UPDATE`, `@Lock(PESSIMISTIC_WRITE)`) al crear y al
actualizar un precio. Las escrituras sobre el mismo producto se serializan, las de productos distintos siguen en
paralelo, y la constraint de exclusión queda como garantía final. El borrado no bloquea porque no puede crear solapes.
El resultado se verifica en `ConcurrentPriceCreationIntegrationTest`.

## Tests

115 tests, todos pasan con `./gradlew test`:

| Clase                                    | Qué cubre                                                                                                       | Tests |
|------------------------------------------|-----------------------------------------------------------------------------------------------------------------|-------|
| `PriceTest`                              | Constructor, `isActive`, `overlaps` (bordes, rangos abiertos, simetría) y `update`                              | 35    |
| `PriceRequestValidationTest`             | Límites de `value`, fechas obligatorias, códigos de moneda                                                      | 26    |
| `CreateProductRequestValidationTest`     | Límites de `name` y `description`                                                                               | 13    |
| `ProductPriceApiIntegrationTest`         | Escenario completo del benchmark, errores HTTP, paginación, filtros, monedas, GET/PUT/DELETE de precio, OpenAPI | 38    |
| `ConcurrentPriceCreationIntegrationTest` | Peticiones simultáneas sobre el mismo producto y sobre productos distintos                                      | 3     |

## Cambios en la infraestructura de la base

| Fichero                 | Cambio                                                                                                                                                                                                                                                                                                                                                                                                                      |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Dockerfile`            | Java 25 con el wrapper de Gradle. Build sin tests (no hay Docker dentro de `docker build`), con capas de dependencias cacheadas. Imagen final sobre JRE, usuario sin privilegios, `-XX:MaxRAMPercentage=75 -XX:+UseSerialGC`. Caché AOT generada durante el build con un arranque de entrenamiento sin base de datos (`spring.context.exit=onRefresh`; Flyway y la validación de Hibernate se desactivan solo en ese paso). |
| `docker-compose.yml`    | Eliminadas la línea `command` (apuntaba a una clase `com.mango.App` inexistente) y la clave `version` obsoleta. Nuevo servicio `db` (PostgreSQL, 0,5 CPU y 1 GB, con healthcheck). Nuevo servicio `k6` en un perfil propio (0,5 CPU y 1 GB). Los límites de `app` y `benchmark` no se han tocado.                                                                                                                           |
| `Dockerfile.benchmark`  | Añadido `bc`; sin él, los tiempos que imprime `benchmark.sh` salen vacíos. `benchmark.sh` no se ha modificado.                                                                                                                                                                                                                                                                                                              |
| `build.gradle`, wrapper | Spring Boot 4.1.2, Java 25, Gradle 9.2.1.                                                                                                                                                                                                                                                                                                                                                                                   |
| Nuevos                  | `.gitignore`, `.dockerignore`, `.editorconfig`, `perf/k6/load-test.js`. `gradlew`                                                                                                                                                                                                                                                                                                                                           |

## Rendimiento

### Arranque

Timeline de tiempos de arranque de la aplicación y su avance:

| Configuración                                | Arranque |
|----------------------------------------------|----------|
| Dockerfile mínimo (JDK, sin flags)           | 18,1 s   |
| JRE + usuario sin privilegios + flags de JVM | 17,4 s   |
| + caché AOT de Java 25                       | 6,9 s    |
| Final, con todo el código                    | 4,1 s    |

La caché AOT es el cambio que importa. Su coste es el tamaño de la imagen (863 MB), un compromiso consciente: el
enunciado valora el arranque, no el tamaño. Al arrancar, la JVM muestra un aviso sobre stubs de la caché AOT; es
inofensivo.

### `benchmark.sh` del enunciado

Resultado de la última ejecución con `docker compose up --build --exit-code-from benchmark`:

| Prueba                               | Tiempo  |
|--------------------------------------|---------|
| 1000 creaciones de producto          | 8,5 s   |
| 20 000 consultas de precio por fecha | 182,5 s |
| 15 000 consultas de historial        | 139,3 s |

La parte funcional del script termina sin errores y los 1000 productos se crean. Estos números hay que leerlos con
cuidado: el script lanza un proceso `curl` por petición desde un contenedor con 1 CPU, y eso es lo que mide. En una
ejecución anterior las tres pruebas iban a la misma velocidad (unas 77 peticiones/s) aunque unas escriben y otras leen,
y k6 obtiene miles de peticiones/s contra los mismos endpoints. La mejora de alrededor del 30 % entre esa ejecución y la
final se debe a la carga del host en cada momento, no a cambios en el código.

### k6

`perf/k6/load-test.js` ejecuta cuatro escenarios consecutivos: un calentamiento de 15 s con 20 usuarios virtuales y,
después, 30 s por endpoint con 50, 50 y 20 usuarios virtuales. k6 corre con 0,5 CPU y 1 GB.

Resultados de la ejecución final y una sin las mejoras.

| Endpoint         | Peticiones/s (final) | p95 (final) | Peticiones/s (anterior) | p95 (anterior) |
|------------------|----------------------|-------------|-------------------------|----------------|
| Precio por fecha | 3 098                | 68 ms       | 1 206                   | 91 ms          |
| Historial        | 3 362                | 56 ms       | 1 797                   | 79 ms          |
| Crear producto   | 2 467                | 54 ms       | 1 262                   | 64 ms          |

En la ejecución final: 278 651 peticiones, 0,00 % de error (4 fallos, todos en la espera inicial al healthcheck antes de
que la aplicación aceptara conexiones).

## Opcionales del enunciado

| Opcional                                        | Estado                                                               |
|-------------------------------------------------|----------------------------------------------------------------------|
| Prueba de rendimiento automatizada              | Hecho: k6 en Docker Compose, además del `benchmark.sh` original.     |
| Documentación con Swagger/OpenAPI               | Hecho.                                                               |
| Scripts para poblar datos de prueba             | Hecho: migración Flyway activada con el perfil `dev`.                |
| Paginación, ordenación y filtrado del historial | Hecho.                                                               |
| Actualizar o eliminar precios                   | Hecho: PUT y DELETE, más GET por id.                                 |
| Varias monedas por precio                       | Hecho: moneda por precio, sin solape entre monedas, EUR por defecto. |
| Autenticación                                   | Descartado: `benchmark.sh` no envía credenciales.                    |

## Mejoras futuras que se podrian implementar

- Imagen nativa con GraalVM para reducir todavía más arranque y memoria.
- Caché de lecturas para el precio vigente para recuperarla más rapido.
- Escenario k6 a ritmo constante para medir latencia en una carga fija.
- Autenticación.
- Conversión entre monedas.
