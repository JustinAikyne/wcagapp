package com.brahos.accessibilitychecker.exception;

public class AccessibilityServiceException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AccessibilityServiceException(String message) {
        super(message);
    }

    public AccessibilityServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
