package com.dtw.errorHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.dtw.errorHandler.error.ApiError;
import com.dtw.errorHandler.error.ApiValidationError;
import com.dtw.errorHandler.exception.EntityAlreadyExistsException;
import com.dtw.errorHandler.exception.EntityNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

	// Generic error
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ApiError> handleException(Exception ex) {
		ex.printStackTrace();
		ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown error", ex);
		return buildResponseEntityApiError(apiError);
	}

	// Entity not found error
	@ExceptionHandler(EntityNotFoundException.class)
	protected ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException ex) {
		ApiError apiError = new ApiError(HttpStatus.NOT_FOUND);
		apiError.setMessage(ex.getMessage());
		return buildResponseEntityApiError(apiError);
	}

	// Entity already exists error
	@ExceptionHandler(EntityAlreadyExistsException.class)
	protected ResponseEntity<ApiError> handleEntityAlreadyExists(EntityAlreadyExistsException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage(ex.getMessage());
		return buildResponseEntityApiError(apiError);
	}

	// Validation error by @Valid
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage("Validation error");
		for (FieldError error : ex.getFieldErrors()) {
			apiError.addSubError(new ApiValidationError(ex.getTarget().getClass(), error));
		}
		return new ResponseEntity<>(apiError, apiError.getStatus());
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
		return buildResponseEntityApiError(apiError);
	}

	// Json parse error
	@ExceptionHandler(JsonProcessingException.class)
	protected ResponseEntity<ApiError> handleJsonProcessing(Exception ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "JSON processing/parse error", ex);
		return buildResponseEntityApiError(apiError);
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
		return buildResponseEntityApiError(apiError);
	}

	// Paging or sort by incorrect query param exception
	@ExceptionHandler(PropertyReferenceException.class)
	protected ResponseEntity<ApiError> handlePropertyReference(PropertyReferenceException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage(ex.getMessage());
		return buildResponseEntityApiError(apiError);
	}

	// Converter error exception
	@ExceptionHandler(ConversionFailedException.class)
	protected ResponseEntity<ApiError> handleConversionFailed(ConversionFailedException ex) {
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage(ex.getMessage());
		return buildResponseEntityApiError(apiError);
	}

	private ResponseEntity<ApiError> buildResponseEntityApiError(ApiError apiError) {
		return new ResponseEntity<>(apiError, apiError.getStatus());
	}
}