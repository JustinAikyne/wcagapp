package com.brahos.accessibilitychecker.exception;

public class GuidelineServiceException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public GuidelineServiceException(String message) {
		super(message);
	}

	public GuidelineServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
