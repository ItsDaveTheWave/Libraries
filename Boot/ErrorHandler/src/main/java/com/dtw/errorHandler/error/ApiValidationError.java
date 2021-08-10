package com.dtw.errorHandler.error;

import javax.validation.ConstraintViolation;

import org.springframework.validation.FieldError;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class ApiValidationError extends ApiSubError {
	
   private String objectType;
   private String field;
   private Object rejectedValue;
   private String message;

   public ApiValidationError(String objectType, String message) {
       this.objectType = objectType;
       this.message = message;
   }
   
   public ApiValidationError(Class<?> clazz, FieldError fieldError) {	   
	   this.objectType = clazz.getSimpleName();
	   this.field = fieldError.getField();
	   this.rejectedValue = fieldError.getRejectedValue();
	   this.message = fieldError.getDefaultMessage();
   }
   
   public ApiValidationError(ConstraintViolation<?> violation) {
	   
	   this.objectType = violation.getRootBeanClass().getSimpleName();
	   this.field = violation.getPropertyPath().toString();	 
	   this.rejectedValue = violation.getInvalidValue();
	   this.message = violation.getMessage();
   }
}