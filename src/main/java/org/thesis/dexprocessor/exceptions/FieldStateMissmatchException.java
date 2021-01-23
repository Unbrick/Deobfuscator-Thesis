package org.thesis.dexprocessor.exceptions;

public class FieldStateMissmatchException extends RuntimeException {

    private String key;
    private int primitiveValue;
    private String stringValue;
    private boolean isObject;

    public FieldStateMissmatchException(String key, int value) {
        super(FieldStateMissmatchException.class.getSimpleName());
        this.key = key;
        this.primitiveValue = value;
    }

    public FieldStateMissmatchException(String key) {
        super(FieldStateMissmatchException.class.getSimpleName());
        this.key = key;
        this.isObject = true;
    }

    public FieldStateMissmatchException(String key, String stringValue) {
        super(FieldStateMissmatchException.class.getSimpleName());
        this.key = key;
        this.stringValue = stringValue;
    }

    public String getKey() {
        return key;
    }

    public int getPrimitiveValue() {
        return primitiveValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public boolean isObject() {
        return isObject;
    }
}
