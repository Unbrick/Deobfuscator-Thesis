package org.thesis.dexprocessor.vm.instancefields;

public class InstanceField {
    int primitiveValue;
    final String name;
    String stringValue;
    FieldState state;

    public InstanceField(String name) {
        this.name = name;
        this.state = FieldState.UNINITIALIZED;
    }

    public InstanceField(String name, FieldState state) {
        this.name = name;
        this.state = state;
    }

    public InstanceField(String name, int primitiveValue) {
        this.primitiveValue = primitiveValue;
        this.name = name;
        this.state = FieldState.PRIMITIVE;
    }

    public InstanceField(String name, String stringValue) {
        this.stringValue = stringValue;
        this.name = name;
        this.state = FieldState.STRING;
    }

    public void setInstanceStateObject() {
        state = FieldState.OBJECT;
    }

    public void setPrimitiveValue(int primitiveValue) {
        this.primitiveValue = primitiveValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public void setState(FieldState state) {
        this.state = state;
    }

    public int getPrimitiveValue() {
        return primitiveValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public FieldState getState() {
        return state;
    }
}
