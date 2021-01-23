package org.thesis.dexprocessor.vm.instancefields;

public enum FieldState {
    UNINITIALIZED(0x0),
    PRIMITIVE(0x1),
    STRING(0x2),
    OBJECT(0x3);

    int value;

    FieldState(int value) {
        this.value = value;
    }
}
