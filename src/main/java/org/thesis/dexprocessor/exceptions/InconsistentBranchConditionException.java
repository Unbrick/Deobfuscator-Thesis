package org.thesis.dexprocessor.exceptions;

public class InconsistentBranchConditionException extends RuntimeException{

    public InconsistentBranchConditionException() {
        super(InconsistentBranchConditionException.class.getSimpleName());
    }
}
