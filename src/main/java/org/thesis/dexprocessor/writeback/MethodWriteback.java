package org.thesis.dexprocessor.writeback;

import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.rewriter.*;
import org.jf.dexlib2.rewriter.Rewriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class MethodWriteback extends RewriterModule {

    private final MethodImplementation impl;
    public MethodWriteback(MethodImplementation impl) {
        this.impl = impl;
    }

    @Nonnull
    @Override
    public Rewriter<Method> getMethodRewriter(@Nonnull Rewriters rewriters) {
        return new MethodRewriter(rewriters) {
            @Nonnull
            @Override
            public Method rewrite(@Nonnull Method method) {
                return new Method() {
                    @Nonnull
                    @Override
                    public String getDefiningClass() {
                        return method.getDefiningClass();
                    }

                    @Nonnull
                    @Override
                    public String getName() {
                        return method.getName();
                    }

                    @Nonnull
                    @Override
                    public List<? extends MethodParameter> getParameters() {
                        return method.getParameters();
                    }

                    @Nonnull
                    @Override
                    public String getReturnType() {
                        return method.getReturnType();
                    }

                    @Override
                    public int getAccessFlags() {
                        return method.getAccessFlags();
                    }

                    @Nonnull
                    @Override
                    public Set<? extends Annotation> getAnnotations() {
                        return method.getAnnotations();
                    }

                    @Nullable
                    @Override
                    public MethodImplementation getImplementation() {
                        return impl;
                    }

                    @Nonnull
                    @Override
                    public List<? extends CharSequence> getParameterTypes() {
                        return method.getParameterTypes();
                    }

                    @Override
                    public int compareTo(@Nonnull MethodReference methodReference) {
                        return method.compareTo(methodReference);
                    }

                    @Override
                    public void validateReference() throws InvalidReferenceException {
                        method.validateReference();
                    }
                };
            }
        };
    }
}
