package org.thesis.dexprocessor.vm.registers;

public class Register {
    int registerIndex;
    int primitiveValue;
    String stringValue;
    RegisterState state;

    public Register(int registerIndex) {
        this.registerIndex = registerIndex;
        this.state = RegisterState.UNINITIALIZED;
    }

    public Register(int registerIndex, int value) {
        this.registerIndex = registerIndex;
        this.primitiveValue = value;
        this.state = RegisterState.PRIMITIVE;
    }

    public Register(int registerIndex, String value) {
        this.registerIndex = registerIndex;
        this.stringValue = value;
        this.state = RegisterState.STRING;
    }

    @Override
    public String toString() {
        if (state == RegisterState.PRIMITIVE)
            return "0x" + Integer.toHexString(primitiveValue).toUpperCase();
        else if (state == RegisterState.STRING)
            return stringValue;
        else if (state == RegisterState.OBJECT)
            return "Object";
        return "UNINITIALIZED";
    }

    public void setStateObject() {
        this.state = RegisterState.OBJECT;
    }

    public void setStateString() {
        this.state = RegisterState.STRING;
    }

    public void setStatePrimitive() {
        this.state = RegisterState.PRIMITIVE;
    }

    public void setState(RegisterState state) {
        this.state = state;
    }

    public void setRegisterIndex(int registerIndex) {
        this.registerIndex = registerIndex;
    }

    public void setPrimitiveValue(int primitiveValue) {
        this.primitiveValue = primitiveValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public int getRegisterIndex() {
        return registerIndex;
    }

    public int getPrimitiveValue() {
        return primitiveValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public RegisterState getState() {
        return state;
    }
}
