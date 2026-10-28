package com.mango.products.infrastructure.web;

import com.mango.products.domain.exception.InvalidHistoryFilterException;
import com.mango.products.domain.exception.InvalidPriceRangeException;
import com.mango.products.domain.exception.PriceNotFoundException;
import com.mango.products.domain.exception.PriceOverlapException;
import com.mango.products.domain.exception.ProductNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Product not found", ex);
    }

    @ExceptionHandler(PriceNotFoundException.class)
    public ProblemDetail handlePriceNotFound(PriceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Price not found", ex);
    }

    @ExceptionHandler(PriceOverlapException.class)
    public ProblemDetail handlePriceOverlap(PriceOverlapException ex) {
        return problem(HttpStatus.CONFLICT, "Price overlap", ex);
    }

    @ExceptionHandler(InvalidPriceRangeException.class)
    public ProblemDetail handleInvalidPriceRange(InvalidPriceRangeException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid price range", ex);
    }

    @ExceptionHandler(InvalidHistoryFilterException.class)
    public ProblemDetail handleInvalidHistoryFilter(InvalidHistoryFilterException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid filter", ex);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Invalid request");

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "invalid value")));
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, String title, RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(title);
        return problem;
    }
}
