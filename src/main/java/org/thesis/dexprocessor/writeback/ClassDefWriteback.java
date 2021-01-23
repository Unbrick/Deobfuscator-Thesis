package org.thesis.dexprocessor.writeback;

import com.google.common.collect.Lists;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.rewriter.ClassDefRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class ClassDefWriteback extends RewriterModule {

    private final Method method;

    public ClassDefWriteback(Method mMethod) {
        this.method = mMethod;
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
                        return replaceMethodIfExists(Lists.newArrayList(mClassDef.getDirectMethods()), method);
                    }

                    @Nonnull
                    @Override
                    public Iterable<? extends Method> getVirtualMethods() {
                        return replaceMethodIfExists(Lists.newArrayList(mClassDef.getVirtualMethods()), method);
                    }

                    @Nonnull
                    @Override
                    public Iterable<? extends Method> getMethods() {
                        return replaceMethodIfExists(Lists.newArrayList(mClassDef.getMethods()), method);
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

    private Iterable<? extends Method> replaceMethodIfExists(List<Method> methods, Method mMethod) {
        IntStream.range(0, methods.size())
                .filter(value -> {
                    Method m = methods.get(value);
                    return m.getName().equals(mMethod.getName())
                            && m.getAccessFlags() == mMethod.getAccessFlags()
                            && m.getParameters().equals(mMethod.getParameters())
                            && m.getReturnType().equals(mMethod.getReturnType());
                })
                .findFirst()
                .ifPresent(value -> methods.set(value, mMethod));
        return methods;
    }
}
