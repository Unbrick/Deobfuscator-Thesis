package org.thesis.dexprocessor.writeback;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderExceptionHandler;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;

import java.util.List;
import java.util.function.Consumer;

import static org.thesis.dexprocessor.writeback.Rewriter.opcodeIsBetween;

public class ExceptionBlockWriter {

    public static List<Integer> getExceptionHandlerInstructionAddresses(MutableMethodImplementation mutableMethodImplementation) {
        List<Integer> mExceptionHandlerAddress = Lists.newArrayList();
        mutableMethodImplementation.getTryBlocks().forEach(builderTryBlock -> builderTryBlock.getExceptionHandlers().forEach((Consumer<BuilderExceptionHandler>) builderExceptionHandler -> {
            int exceptionHandlerIndex = builderExceptionHandler.getHandler().getLocation().getIndex();
            int size = getExceptionHandlerLength(exceptionHandlerIndex, mutableMethodImplementation);
            int exceptionHandlerEndAddress = exceptionHandlerIndex + size;
            for (int i = exceptionHandlerIndex; i <= exceptionHandlerEndAddress; i++) {
                mExceptionHandlerAddress.add(i);
            }
        }));
        return mExceptionHandlerAddress;
    }

    public static int getExceptionHandlerLength(int startAddress, MutableMethodImplementation methodImplementation) {
        List<Instruction> mInstructions = Lists.newArrayList(methodImplementation.getInstructions());
        for (int i = startAddress; i < mInstructions.size(); i++) {
            Instruction mCurrentInstruction = mInstructions.get(i);
            Opcode mCurrentOpcode = mCurrentInstruction.getOpcode();
            if (mCurrentOpcode.equals(Opcode.THROW) || opcodeIsBetween(Opcode.RETURN_VOID, Opcode.RETURN_OBJECT, mCurrentOpcode)) {
                return i - startAddress;
            }
        }
        return 0;
    }
}
