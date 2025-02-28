package com.brahos.accessibilitychecker.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AccessibilityServiceException.class)
	public ResponseEntity<String> handleAccessibilityServiceException(AccessibilityServiceException ex) {
		// Log the exception (optional)
		// logger.error("Error executing accessibility guidelines", ex);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("An error occurred while executing accessibility guidelines: " + ex.getMessage());
	}
}
