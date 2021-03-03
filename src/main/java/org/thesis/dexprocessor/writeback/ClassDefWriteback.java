package org.thesis.dexprocessor.writeback;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.rewriter.ClassDefRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;
import org.thesis.Logger;
import org.thesis.dexprocessor.FormatHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ClassDefWriteback extends RewriterModule {

    private final List<Method> deobfuscatedMethods;

    public ClassDefWriteback(List<Method> mMethods) {
        this.deobfuscatedMethods = mMethods;
    }

    @Nonnull
    @Override
    public Rewriter<ClassDef> getClassDefRewriter(@Nonnull Rewriters rewriters) {
        return new ClassDefRewriter(rewriters) {
            @Nonnull
            @Override
            public ClassDef rewrite(@Nonnull ClassDef mClassDef) {
                return new ClassDef() {
                    @Nonnull
                    @Override
                    public String getType() {
                        return mClassDef.getType();
                    }

                    @Override
                    public int getAccessFlags() {
                        return mClassDef.getAccessFlags();
                    }

                    @Nullable
                    @Override
                    public String getSuperclass() {
                        return mClassDef.getSuperclass();
                    }

                    @Nonnull
                    @Override
                    public List<String> getInterfaces() {
                        return mClassDef.getInterfaces();
                    }

                    @Nullable
                    @Override
                    public String getSourceFile() {
                        return mClassDef.getSourceFile();
                    }

                    @Nonnull
                    @Override
                    public Set<? extends Annotation> getAnnotations() {
                        return mClassDef.getAnnotations();
                    }

                    @Nonnull
                    @Override
                    public Iterable<? extends Field> getStaticFields() {
                        return mClassDef.getStaticFields();
                    }

                    @Nonnull
                    @Override
                    public Iterable<? extends Field> getInstanceFields() {
                        return mClassDef.getInstanceFields();
                    }

                    @Nonnull
                    @Override
                    public Iterable<? extends Field> getFields() {
                        return mClassDef.getFields();
                    }

                    @Nonnull
                    @Override
                    public Iterable<? extends Method> getDirectMethods() {
                        return getMethodsOfType(mClassDef, deobfuscatedMethods, MethodType.DIRECT);
                    }

                    @Nonnull
                    @Override
                    public Iterable<? extends Method> getVirtualMethods() {
                        return getMethodsOfType(mClassDef, deobfuscatedMethods, MethodType.VIRTUAL);
                    }

                    @Nonnull
                    @Override
                    public Iterable<? extends Method> getMethods() {
                        Logger.debug("Writeback", "Replacing deobfuscated methods of class " + FormatHelper.getClassSimpleName(mClassDef));
                        return deobfuscatedMethods;
                    }

                    @Override
                    public int compareTo(@Nonnull CharSequence charSequence) {
                        return mClassDef.compareTo(charSequence);
                    }

                    @Override
                    public int length() {
                        return mClassDef.length();
                    }

                    @Override
                    public char charAt(int index) {
                        return mClassDef.charAt(index);
                    }

                    @Override
                    public CharSequence subSequence(int start, int end) {
                        return mClassDef.subSequence(start,end);
                    }

                    @Override
                    public void validateReference() throws InvalidReferenceException {
                        mClassDef.validateReference();
                    }
                };
            }
        };
    }

    public enum MethodType {
        DIRECT,
        VIRTUAL;
    }

    private ArrayList<? extends Method> getMethodsOfType(ClassDef mClass, List<Method> methods, MethodType methodType) {
        return methods.stream().filter(m -> getMethodType(mClass, m) == methodType).collect(Collectors.toCollection(ArrayList::new));
    }


    private MethodType getMethodType(ClassDef mClass, Method methodInQuestion) {
        AtomicReference<MethodType> methodType = new AtomicReference<>();

        mClass.getDirectMethods().forEach((Consumer<Method>) method -> {
            if (method.getName().equals(methodInQuestion.getName())
                    && method.getAccessFlags() == methodInQuestion.getAccessFlags()
                    && method.getParameters().equals(methodInQuestion.getParameters())
                    && method.getReturnType().equals(methodInQuestion.getReturnType()))
                methodType.set(MethodType.DIRECT);
        });

        mClass.getVirtualMethods().forEach((Consumer<Method>) method -> {
            if (method.getName().equals(methodInQuestion.getName())
                    && method.getAccessFlags() == methodInQuestion.getAccessFlags()
                    && method.getParameters().equals(methodInQuestion.getParameters())
                    && method.getReturnType().equals(methodInQuestion.getReturnType()))
                methodType.set(MethodType.VIRTUAL);
        });

        Logger.debug("Found method type: ", String.valueOf(methodType));

        return methodType.get();
    }
}
