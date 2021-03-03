package org.thesis.dexprocessor;

import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.instruction.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FormatHelper {

    private static Map<String, String> primitiveToName = new HashMap<>() {{
        put("I", "int");
        put("Z", "bool");
        put("S", "short");
        put("C", "char");
        put("J", "long");
        put("F", "float");
        put("D", "double");
    }};

    public static String fieldTypeToString(String field) {
        String returnValue;
        if (field.length() == 1) {
            return primitiveToName.get(field);
        } else if (field.indexOf("[") == 0 && field.length() == 2) {
            return primitiveToName.get(field.substring(0,1)) + "[]";
        } else if (field.contains("/")) {
            return field.substring(field.lastIndexOf('/') + 1).replace(";", "");
        } else System.out.println("Unknown field type: " + field);
        return field;
    }

    public static String hex(int value) {
        if (value < 0)
            return "-0x" + Long.toHexString(Math.abs(value));
        else
            return "0x" + Long.toHexString(Math.abs(value));
    }

    public static String instructionToString(Instruction mInstruction) {
        StringBuilder mBuilder = new StringBuilder();
        /*mBuilder.append("(").append(mInstruction.getCodeUnits()).append(", ");
        mBuilder.append(mInstruction.getClass().getSimpleName());
        mBuilder.append(")");*/
        if (mInstruction.getClass().getInterfaces().length > 0) {
            Class baseInterface = mInstruction.getClass().getInterfaces()[0];
            ArrayList<String> mInterfaces = new ArrayList<>();
            for (Class anInterface : baseInterface.getInterfaces()) {
                mInterfaces.add(anInterface.getSimpleName());
            }
            mBuilder.append(mInterfaces);
        } else {
            mBuilder.append(mInstruction.getClass().getSimpleName());
        }

        String opcodeName = mInstruction.getOpcode().name();
        String smaliOpcodeName = opcodeName.toLowerCase().replaceFirst("_", "-").replaceFirst("_", "/");
        mBuilder.append(" ");
        mBuilder.append(smaliOpcodeName);
        if (mInstruction instanceof OneRegisterInstruction) {
            mBuilder.append(" v").append(((OneRegisterInstruction) mInstruction).getRegisterA());
        }
        if (mInstruction instanceof TwoRegisterInstruction) {
            mBuilder.append(", v").append(((TwoRegisterInstruction) mInstruction).getRegisterB());
        }
        if (mInstruction instanceof ThreeRegisterInstruction) {
            mBuilder.append(", v").append(((ThreeRegisterInstruction) mInstruction).getRegisterC());
        }
        if (mInstruction instanceof ReferenceInstruction) {
            mBuilder.append(", ").append(((ReferenceInstruction) mInstruction).getReference().toString());
        }
        if (mInstruction instanceof WideLiteralInstruction) {
            long literal = ((WideLiteralInstruction) mInstruction).getWideLiteral();
            if (literal < 0)
                mBuilder.append(", -");
            else mBuilder.append(", ");
            mBuilder.append(hex((int) literal));
        }
        if (mInstruction instanceof OffsetInstruction) {
            try {
                int codeOffset = ((OffsetInstruction) mInstruction).getCodeOffset();
                mBuilder.append(codeOffset > 0 ? ", +" : ", -");
                mBuilder.append(codeOffset);
            } catch (IllegalStateException e) {
                mBuilder.append(", NOT ADDED TO METHOD");
            }
        }

        return mBuilder.toString();
    }

    public static String getClassSimpleName(ClassDef mClassDef) {
        String fullClassDef = mClassDef.getType();
        int lastSlash = mClassDef.getType().lastIndexOf("/");
        return fullClassDef.substring(lastSlash + 1, fullClassDef.length() - 1);
    }

    public static void printInstruction(Instruction mInstruction) {
        System.out.println(FormatHelper.instructionToString(mInstruction));
    }
}
