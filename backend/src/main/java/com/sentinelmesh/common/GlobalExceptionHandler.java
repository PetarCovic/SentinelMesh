package com.sentinelmesh.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sentinelmesh.exceptions.AlertNotFoundException;
import com.sentinelmesh.exceptions.DeviceNotFoundException;
import com.sentinelmesh.exceptions.InvalidDeviceApiKeyException;
import com.sentinelmesh.exceptions.SecurityEventNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler 
{
	@ExceptionHandler(DeviceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> 
		handleDeviceNotFound(DeviceNotFoundException ex)
	{
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
				"timestamp", Instant.now().toString(),
				"status", 404,
				"error", "Not Found",
				"message", ex.getMessage()));
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(
			MethodArgumentNotValidException ex)
	{
		List<String> validationErrors=ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> error.getField()+ ": "+error.getDefaultMessage())
				.toList();
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
				"timestamp", Instant.now().toString(),
				"status", 400,
				"error", "Validation Failed",
				"messages", validationErrors));
	}
	
	@ExceptionHandler(InvalidDeviceApiKeyException.class)
	public ResponseEntity<Map<String, Object>> handleInvalidDeviceApiKey(
	        InvalidDeviceApiKeyException ex
	) {
	    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
	            "timestamp", Instant.now().toString(),
	            "status", 401,
	            "error", "Unauthorized",
	            "message", ex.getMessage()
	    ));
	}
	
	@ExceptionHandler(SecurityEventNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleSecurityEventNotFound(
	        SecurityEventNotFoundException ex
	) {
	    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
	            "timestamp", Instant.now().toString(),
	            "status", 404,
	            "error", "Not Found",
	            "message", ex.getMessage()
	    ));
	}
	
	@ExceptionHandler(AlertNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleAlertNotFound(AlertNotFoundException ex) {
	    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
	            "timestamp", Instant.now().toString(),
	            "status", 404,
	            "error", "Not Found",
	            "message", ex.getMessage()
	    ));
	}
}
