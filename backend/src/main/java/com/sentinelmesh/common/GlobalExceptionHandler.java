package com.sentinelmesh.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sentinelmesh.exceptions.DeviceNotFoundException;

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
}
