package com.sentinelmesh.exceptions;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler 
{
	@ExceptionHandler(DeviceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleDeviceNotFound(DeviceNotFoundException ex)
	{
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
				"timestamp", Instant.now().toString(),
				"status", 404,
				"error", "Not Found",
				"message", ex.getMessage()));
	}
		
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex)
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
	public ResponseEntity<Map<String, Object>> handleInvalidDeviceApiKey(InvalidDeviceApiKeyException ex) 
	{
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
				"timestamp", Instant.now().toString(),
		    	"status", 401,
		      	"error", "Unauthorized",
		      	"message", ex.getMessage()));
	}
		
	@ExceptionHandler(SecurityEventNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleSecurityEventNotFound(
			SecurityEventNotFoundException ex
			) 
	{
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
		    	"timestamp", Instant.now().toString(),
		     	"status", 404,
		      	"error", "Not Found",
		      	"message", ex.getMessage()));
	}
		
	@ExceptionHandler(AlertNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleAlertNotFound(AlertNotFoundException ex) 
	{
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
		      	"timestamp", Instant.now().toString(),
		     	"status", 404,
		      	"error", "Not Found",
		       	"message", ex.getMessage()));
	}
		
	@ExceptionHandler(AlertRuleNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleAlertRuleNotFound(AlertRuleNotFoundException ex) 
	{
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
				"timestamp", Instant.now().toString(),
		      	"status", 404,
		      	"error", "Not Found",
		      	"message", ex.getMessage()));
	}

	@ExceptionHandler(VideoClipAlreadyExistsException.class)
	public ResponseEntity<Map<String, Object>> handleVideoClipAlreadyExists(
			VideoClipAlreadyExistsException ex
			) 
	{
	    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
	            "timestamp", Instant.now().toString(),
	            "status", 409,
	            "error", "Conflict",
	            "message", ex.getMessage()
	    ));
	}
	
	@ExceptionHandler(VideoClipOwnershipException.class)
	public ResponseEntity<Map<String, Object>> handleVideoClipOwnership(VideoClipOwnershipException ex) 
	{
	    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
	            "timestamp", Instant.now().toString(),
	            "status", 403,
	            "error", "Forbidden",
	            "message", ex.getMessage()
	    ));
	}
	
	@ExceptionHandler(VideoClipStorageException.class)
	public ResponseEntity<Map<String, Object>> handleVideoClipStorage(VideoClipStorageException ex) 
	{
	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
	            "timestamp", Instant.now().toString(),
	            "status", 500,
	            "error", "Video Clip Storage Error",
	            "message", ex.getMessage()
	    ));
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) 
	{
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
	            "timestamp", Instant.now().toString(),
	            "status", 400,
	            "error", "Bad Request",
	            "message", ex.getMessage()
	    ));
	}
	
	@ExceptionHandler(RecordingSegmentNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleRecordingSegmentNotFound(
	        RecordingSegmentNotFoundException ex)
	{
	    return ResponseEntity.status(HttpStatus.NOT_FOUND)
	            .body(createErrorResponse(
	                    HttpStatus.NOT_FOUND,
	                    ex.getMessage()
	            ));
	}

	@ExceptionHandler(RecordingSegmentValidationException.class)
	public ResponseEntity<Map<String, Object>> handleRecordingSegmentValidation(
	        RecordingSegmentValidationException ex)
	{
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .body(createErrorResponse(
	                    HttpStatus.BAD_REQUEST,
	                    ex.getMessage()
	            ));
	}

	@ExceptionHandler(RecordingSegmentOwnershipException.class)
	public ResponseEntity<Map<String, Object>> handleRecordingSegmentOwnership(
	        RecordingSegmentOwnershipException ex)
	{
	    return ResponseEntity.status(HttpStatus.FORBIDDEN)
	            .body(createErrorResponse(
	                    HttpStatus.FORBIDDEN,
	                    ex.getMessage()
	            ));
	}

	@ExceptionHandler(RecordingSegmentStorageException.class)
	public ResponseEntity<Map<String, Object>> handleRecordingSegmentStorage(
	        RecordingSegmentStorageException ex)
	{
	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body(createErrorResponse(
	                    HttpStatus.INTERNAL_SERVER_ERROR,
	                    ex.getMessage()
	            ));
	}
	
	private Map<String, Object> createErrorResponse(
	        HttpStatus status,
	        String message)
	{
	    return Map.of(
	            "timestamp", Instant.now().toString(),
	            "status", status.value(),
	            "error", status.getReasonPhrase(),
	            "message", message
	    );
	}
	
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(
	        MaxUploadSizeExceededException ex)
	{
	    HttpStatus status = HttpStatus.PAYLOAD_TOO_LARGE;

	    return ResponseEntity.status(status)
	            .body(createErrorResponse(
	                    status,
	                    "Uploaded file exceeds the maximum permitted size"
	            ));
	}
}
