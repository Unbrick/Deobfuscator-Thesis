package org.thesis.dexprocessor.exceptions;

public class MethodNotFoundException extends RuntimeException {

    public MethodNotFoundException(String message) {
        super(message);
    }
}
