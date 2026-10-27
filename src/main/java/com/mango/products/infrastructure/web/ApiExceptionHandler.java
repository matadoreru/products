package com.mango.products.infrastructure.web;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.mango.products.domain.ProductNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler{

	@ExceptionHandler(ProductNotFoundException.class)
	public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Product not found");
		return problem;
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

			ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "invalid value")));
			problem.setProperty("errors", errors);

			return ResponseEntity.badRequest().body(problem);
		}
}
