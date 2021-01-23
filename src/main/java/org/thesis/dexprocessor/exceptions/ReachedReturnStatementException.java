package org.thesis.dexprocessor.exceptions;

public class ReachedReturnStatementException extends RuntimeException {

    public ReachedReturnStatementException() {
        super(ReachedReturnStatementException.class.getSimpleName());
    }
}
