package com.dtw.errorHandler.error;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Data;

@Data
public class ApiError {
	
	private HttpStatus status;
	@JsonFormat(shape = Shape.STRING, pattern = "HH:mm yyyy/MM/dd")
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDateTime timestamp;
	private String message;
	private String debugMessage;
	private List<ApiSubError> subErrors = new ArrayList<>();

	private ApiError() {
	       timestamp = LocalDateTime.now();
	}

	public ApiError(HttpStatus status) {
		this();
		this.status = status;
	}

	public ApiError(HttpStatus status, Throwable ex) {
		this();
		this.status = status;
		this.message = "Unexpected error";
		this.debugMessage = ex.getLocalizedMessage();
	}

	public ApiError(HttpStatus status, String message, Throwable ex) {
		this();
		this.status = status;
		this.message = message;
		this.debugMessage = ex.getLocalizedMessage();
	}

	public void addSubError(ApiSubError subError) {
		this.subErrors.add(subError);
	}
	
	public ResponseEntity<ApiError> buildResponseEntity() {
		return new ResponseEntity<>(this, this.getStatus());
	}
	
	public static ApiError entityNotFound(String resourceName, String identifierName, Object indentifierValue) {
		ApiError apiError = new ApiError(HttpStatus.NOT_FOUND);
		apiError.setMessage(resourceName + " with " + identifierName + " [" + indentifierValue + "] not found");
		return apiError;
	}
	
	public static ApiError entityAlreadyExists(String resourceName, String identifierName, Object indentifierValue) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage(resourceName + " with " + identifierName + " [" + indentifierValue + "] already exists");
		return apiError;
	}
	
	public static ApiError entityDoesntContainEntity(String parentResourceName, String childResourceName, String identifierName, Object indentifierValue) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage(parentResourceName + " does not contain " + childResourceName + " with " + identifierName + " [" + indentifierValue + "]");
		return apiError;
	}
}