package org.thesis.dexprocessor.vm;

import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.thesis.dexprocessor.exceptions.MethodNotFoundException;

import java.util.List;
import java.util.Objects;

public class MethodManager {
    private Method mMethod;
    private MutableMethodImplementation mImplementation;
    private ClassDef mClass;

    public MethodManager(ClassDef mClass, Method mMethod, MutableMethodImplementation mImplementation) {
        this.mClass = mClass;
        this.mMethod = mMethod;
        if (mMethod != null && mMethod.getImplementation() != null) {
            this.mImplementation = mImplementation;
        }
    }

    public BuilderInstruction getInstructionAt(int index) {
        return mImplementation.getInstructions().get(index);
    }

    public List<BuilderInstruction> getInstructions() {
        return mImplementation.getInstructions();
    }

    public boolean hasNextInstruction(int index) {
        return index != mImplementation.getInstructions().size() - 1;
    }

    public boolean selectMethod(Method method, MutableMethodImplementation mImplementation) {
        mMethod = method;
        mImplementation = mImplementation;
        return true;
    }

    public boolean selectMethod(String name, List<? extends CharSequence> parameters, String returnType) {
        for (Method method : mClass.getMethods()) {
            if (method.getName().equals(name)
            && method.getParameterTypes().equals(parameters)
            && method.getReturnType().equals(returnType)
            && method.getImplementation() != null) {
                return selectMethod(method, new MutableMethodImplementation(method.getImplementation()));
            }
        }
        throw new MethodNotFoundException("Could not find method " + name);
    }

    public BuilderInstruction getNextInstruction(int currentInstructionIndex) {
        return getInstructionAt(currentInstructionIndex);
    }

    public String getMethodName() {
        return mMethod.getName();
    }

    public Method getMethod() {
        return mMethod;
    }
}
