package org.thesis.dexprocessor.vm.registers;

public enum RegisterState {
    UNINITIALIZED(0x0),
    PRIMITIVE(0x1),
    STRING(0x2),
    OBJECT(0x3);

    private final int value;

    RegisterState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
