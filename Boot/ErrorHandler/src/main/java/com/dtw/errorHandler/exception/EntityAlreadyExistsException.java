package com.dtw.errorHandler.exception;

public class EntityAlreadyExistsException extends Exception{

	private static final long serialVersionUID = 1L;
	
	public EntityAlreadyExistsException(Class<?> clazz) {
		super(clazz.getSimpleName() + " already exists");
	}
	
	public EntityAlreadyExistsException(Class<?> clazz, Long id) {
		super(clazz.getSimpleName() + " with id [" + id + "] already exists");
	}
	
	public EntityAlreadyExistsException(Class<?> clazz, String uniqueIdentifierName, String uniqueIdentifierValue) {
		super(clazz.getSimpleName() + " with " + uniqueIdentifierName + " [" + uniqueIdentifierValue + "] already exists");
	}
}