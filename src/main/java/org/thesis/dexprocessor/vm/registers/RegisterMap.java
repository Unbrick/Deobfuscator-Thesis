package org.thesis.dexprocessor.vm.registers;

import java.util.HashMap;

public class RegisterMap extends HashMap<Integer, Register> {

    public RegisterMap(int registerCount) {
        init(registerCount);
    }

    private void init(int registerCount) {
        for (int i = 0; i < registerCount; i++) {
            this.put(i, new Register(i));
        }
    }

    public Register get(int key) {
        return super.get(key);
    }

    public Register put(int key, int value) {
        return super.put(key, new Register(key, value));
    }

    public Register put(int key, String value) {
        return super.put(key, new Register(key, value));
    }

    public Register putUninitialized(int key) {
        return super.put(key, new Register(key));
    }

    public Register putObject(int key) {
        Register mRegister = new Register(key);
        mRegister.setStateObject();
        return super.put(key, mRegister);
    }

    public void clearAndInit() {
        int registerCount = size();
        clear();
        init(registerCount);
    }

    @Override
    public String toString() {
        StringBuilder mBuilder = new StringBuilder();
        this.forEach((integer, register) -> {
            if (register.getState() != RegisterState.UNINITIALIZED)
                mBuilder.append("\r\n\t[").append(integer).append(": ").append(register).append("]");
        });
        return mBuilder.toString();
    }
}
