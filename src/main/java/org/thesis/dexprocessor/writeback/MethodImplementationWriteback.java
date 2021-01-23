package org.thesis.dexprocessor.writeback;

import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.rewriter.MethodImplementationRewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

import javax.annotation.Nonnull;
import java.util.List;

public class MethodImplementationWriteback extends RewriterModule {

    private final List<TryBlock<? extends ExceptionHandler>> mTryBlocks;

    public MethodImplementationWriteback(List<TryBlock<? extends ExceptionHandler>> mTryBlocks) {
        this.mTryBlocks = mTryBlocks;
    }

    @Nonnull
    @Override
    public MethodImplementationRewriter getMethodImplementationRewriter(@Nonnull Rewriters rewriters) {
        return new MethodImplementationRewriter(rewriters) {
            @Nonnull
            @Override
            public MethodImplementation rewrite(@Nonnull MethodImplementation methodImplementation) {
                return new MethodImplementation() {
                    @Override
                    public int getRegisterCount() {
                        return methodImplementation.getRegisterCount();
                    }

                    @Nonnull
                    @Override
                    public Iterable<? extends Instruction> getInstructions() {
                        return methodImplementation.getInstructions();
                    }

                    @Nonnull
                    @Override
                    public List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks() {
                        return mTryBlocks;
                    }

                    @Nonnull
                    @Override
                    public Iterable<? extends DebugItem> getDebugItems() {
                        return methodImplementation.getDebugItems();
                    }
                };
            }
        };
    }
}
