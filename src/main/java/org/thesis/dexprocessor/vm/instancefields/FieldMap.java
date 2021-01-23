package org.thesis.dexprocessor.vm.instancefields;

import org.jf.dexlib2.iface.Field;
import org.thesis.dexprocessor.exceptions.FieldStateMissmatchException;

import java.util.HashMap;

public class FieldMap extends HashMap<String, InstanceField> {

    private Iterable<? extends Field> fields;

    public FieldMap(Iterable<? extends Field> fields) {
        this.fields = fields;
        init(fields);
    }

    private void init(Iterable<? extends Field> fields) {
        for (Field field : fields) {
            String fieldName = field.getName();
            if (field.getType().length() <= 2) {
                put(fieldName, FieldState.PRIMITIVE);
            } else if (field.getType().equals("java.lang.String")) {
                put(fieldName, FieldState.STRING);
            } else {
                put(fieldName, FieldState.OBJECT);
            }
        }
    }


    public InstanceField get(String key) {
        return super.get(key);
    }

    public InstanceField put(String key, int value) {
        InstanceField mOldField = get(key);
        if (mOldField == null) {
            return super.put(key, new InstanceField(key, value));
        } else {
            if (mOldField.getState() == FieldState.PRIMITIVE || mOldField.getState() == FieldState.UNINITIALIZED) {
                //okay we got a primitive/uninitialized field, lets proceed
                return super.put(key, new InstanceField(key, value));
            } else {
                // old field state does not match out given value
                throw new FieldStateMissmatchException(key, value);
            }
        }
    }

    public InstanceField put(String key, FieldState value) {
        return super.put(key, new InstanceField(key, value));
    }

    public InstanceField put(String key, String value) {
        InstanceField mOldField = get(key);
        if (mOldField == null) {
            return super.put(key, new InstanceField(key, value));
        } else {
            if (mOldField.getState() == FieldState.STRING || mOldField.getState() == FieldState.UNINITIALIZED) {
                //okay we got a primitive/uninitialized field, lets proceed
                return super.put(key, new InstanceField(key, value));
            } else {
                // old field state does not match out given value
                throw new FieldStateMissmatchException(key, value);
            }
        }
    }

    public InstanceField putObject(String key) {
        InstanceField mOldField = get(key);
        if (mOldField == null) {
            return super.put(key, new InstanceField(key));
        } else {
            if (mOldField.getState() == FieldState.OBJECT || mOldField.getState() == FieldState.UNINITIALIZED) {
                //okay we got a primitive/uninitialized field, lets proceed
                return super.put(key, new InstanceField(key, FieldState.OBJECT));
            } else {
                // old field state does not match out given value
                throw new FieldStateMissmatchException(key);
            }
        }
    }

    public void clearAndInit() {
        clear();
        init(this.fields);
    }
}
