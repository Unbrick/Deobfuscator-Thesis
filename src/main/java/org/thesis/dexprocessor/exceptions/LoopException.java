package org.thesis.dexprocessor.exceptions;

public class LoopException extends RuntimeException {
    private final int loopStartIndex;

    public LoopException(Integer integer) {
        super(LoopException.class.getSimpleName());
        this.loopStartIndex = integer;
    }

    public int getLoopStartIndex() {
        return loopStartIndex;
    }
}
