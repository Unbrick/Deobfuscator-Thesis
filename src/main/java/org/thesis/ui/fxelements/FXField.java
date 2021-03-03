package org.thesis.ui.fxelements;

import javafx.beans.property.SimpleStringProperty;
import org.jf.dexlib2.iface.Field;
import org.thesis.dexprocessor.FormatHelper;

public class FXField {
    private final SimpleStringProperty FieldName;
    private final SimpleStringProperty FieldType;
    private final SimpleStringProperty FieldValue;
    private final Field mField;

    public FXField(Field mField) {
        this.mField = mField;
        this.FieldName = new SimpleStringProperty(mField.getName());
        this.FieldType = new SimpleStringProperty(FormatHelper.fieldTypeToString(mField.getType()));
        this.FieldValue = new SimpleStringProperty("0");
    }

    public String getFieldName() {
        return FieldName.get();
    }

    public void setFieldName(String mFieldName) {
        this.FieldName.set(mFieldName);
    }

    public String getFieldType() {
        return FieldType.get();
    }

    public void setFieldType(String mFieldType) {
        this.FieldType.set(mFieldType);
    }

    public String getFieldValue() {
        return FormatHelper.hex(Integer.parseInt(FieldValue.get()));
    }

    public void setFieldValue(String mFieldValue) {
        this.FieldValue.set(mFieldValue);
    }
}
