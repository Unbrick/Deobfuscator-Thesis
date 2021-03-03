package org.thesis.dexprocessor.vm.instancefields;

import java.util.EnumSet;
import java.util.Set;

public enum FieldState {
    UNINITIALIZED(0x0),
    PRIMITIVE(0x1),
    STRING(0x2),
    OBJECT(0x3);

    int value;

    FieldState(int value) {
        this.value = value;
    }

    public boolean isPrimitive() {
        return EnumSet.of(PRIMITIVE).contains(value);
    }

    public boolean isNonPrimitive() {
        return EnumSet.of(STRING, OBJECT).contains(value);
    }
}
