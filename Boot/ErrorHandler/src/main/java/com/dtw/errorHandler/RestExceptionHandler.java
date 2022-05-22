package com.dtw.errorHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.dtw.errorHandler.error.ApiError;
import com.dtw.errorHandler.error.ApiValidationError;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler {

	// Generic error
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ApiError> handleException(Exception ex) {
		ex.printStackTrace();
		ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown error", ex);
		return apiError.buildResponseEntity();
	}
	
	// Path not found error
	@ExceptionHandler(NoHandlerFoundException.class)
	protected ResponseEntity<ApiError> handleNoHandlerFound(NoHandlerFoundException ex) {
		ApiError apiError = new ApiError(HttpStatus.NOT_FOUND);
		apiError.setMessage("Path '" + ex.getRequestURL() + "' not found");
		return apiError.buildResponseEntity();
	}
	
	// Required query parameter not present error
	@ExceptionHandler(MissingServletRequestParameterException.class)
	protected ResponseEntity<ApiError> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage("Required query parameter '" + ex.getParameterName() + "' not present");
		return apiError.buildResponseEntity();
	}
	
	// Message not readable error
	@ExceptionHandler(HttpMessageNotReadableException.class)
	protected ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
		if(ex.contains(InvalidFormatException.class) && ex.getCause() instanceof InvalidFormatException) {
			return handleInvalidFormat((InvalidFormatException) ex.getCause());
		}
		if(ex.contains(JsonParseException.class) && ex.getCause() instanceof JsonParseException) {
			return handleJsonParse((JsonParseException) ex.getCause());
		}
		
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage("Message not readable error");
		apiError.setDebugMessage(ex.getMessage().split(";")[0]);
		return apiError.buildResponseEntity();
	}
	
	// Deserialization(parsing) error
	@ExceptionHandler(InvalidFormatException.class)
	protected ResponseEntity<ApiError> handleInvalidFormat(InvalidFormatException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage("Invalid format error");
		if(ex.getPath().isEmpty()) {
			apiError.setDebugMessage(ex.getMessage());
		}
		else {
			apiError.addSubError(new ApiValidationError(ex.getPath().get(0).getFieldName(), ex.getTargetType().getCanonicalName(), ex.getValue(), ex.getOriginalMessage()));
		}
		return apiError.buildResponseEntity();
	}
	
	// JSON malformed error
	@ExceptionHandler(JsonParseException.class)
	protected ResponseEntity<ApiError> handleJsonParse(JsonParseException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage("Invalid JSON error");
		apiError.setDebugMessage(ex.getOriginalMessage());
		return apiError.buildResponseEntity();
	}
	
	// JSON malformed error
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	protected ResponseEntity<ApiError> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		ApiError apiError = new ApiError(HttpStatus.METHOD_NOT_ALLOWED);
		apiError.setMessage("Request method not supported in this URI");
		apiError.setDebugMessage(ex.getMessage());
		return apiError.buildResponseEntity();
	}
	
	// JSON malformed error
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	protected ResponseEntity<ApiError> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage("Media type not suported");
		apiError.setDebugMessage(ex.getMessage());
		return apiError.buildResponseEntity();
	}
	
	// Validation error by @Valid
	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage("Validation error");
		for (FieldError error : ex.getFieldErrors()) {
			apiError.addSubError(new ApiValidationError(ex.getTarget().getClass(), error));
		}
		return apiError.buildResponseEntity();
	}
	

	// Can be thrown when commiting changes to DB, can contain
	// ConstraintViolationException(if error is because of validation)
	@ExceptionHandler(TransactionSystemException.class)
	protected ResponseEntity<ApiError> handleTransactionSystem(TransactionSystemException ex) {
		if (ex.getRootCause() instanceof ConstraintViolationException) {
			return handleConstraintViolation((ConstraintViolationException) ex.getRootCause());
		} else {
			return handleException(ex);
		}
	}

	// Validation error by Validator
	@ExceptionHandler(ConstraintViolationException.class)
	protected ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage("Validation error");
		for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
			apiError.addSubError(new ApiValidationError(violation));
		}
		return apiError.buildResponseEntity();
	}

	// Json parse error
	@ExceptionHandler(JsonProcessingException.class)
	protected ResponseEntity<ApiError> handleJsonProcessing(Exception ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "JSON processing/parse error", ex);
		return apiError.buildResponseEntity();
	}

	// Type conversion error on controller method argument
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	protected ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String message = "Type conversion error.";
		Object value = ex.getValue();
		Class<?> requiredType = ex.getRequiredType();

		if (value != null && requiredType != null) {
			message = message + " The value {" + value + "} can not be converted to the type {"
					+ requiredType.getSimpleName() + "}.";
		}

		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, message, ex);
		return apiError.buildResponseEntity();
	}

	// Paging or sort by incorrect query param exception
	@ExceptionHandler(PropertyReferenceException.class)
	protected ResponseEntity<ApiError> handlePropertyReference(PropertyReferenceException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage(ex.getMessage());
		return apiError.buildResponseEntity();
	}

	// Converter error exception
	@ExceptionHandler(ConversionFailedException.class)
	protected ResponseEntity<ApiError> handleConversionFailed(ConversionFailedException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage(ex.getMessage());
		return apiError.buildResponseEntity();
	}
}