package org.thesis.dexprocessor.writeback;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.*;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21s;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.thesis.MainApp;
import org.thesis.dexprocessor.FormatHelper;
import org.thesis.dexprocessor.instructions.GotoInstruction;
import org.thesis.dexprocessor.instructions.NopInstruction;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Rewriter{

    public static ClassDef rewriteClassDef(ClassDef mClass, Method mMethod) {
        return new DexRewriter(new ClassDefWriteback(mMethod)).getClassDefRewriter().rewrite(mClass);
    }

    public static Method rewriteMethod(Method method, MethodImplementation methodImplementation,List<TryBlock<? extends ExceptionHandler>> mTryBlocks) {
        MethodImplementationWriteback mMethodImplementationWriteback = new MethodImplementationWriteback(mTryBlocks);
        MethodImplementation mImplemetaton = new DexRewriter(mMethodImplementationWriteback).getMethodImplementationRewriter().rewrite(methodImplementation);

        MethodWriteback mMethodWriteback = new MethodWriteback(mImplemetaton);

        return new DexRewriter(mMethodWriteback).getMethodRewriter().rewrite(method);
    }

    public static MethodImplementation fillWithNops(MethodImplementation implementation, int startIndex, int endIndex) {
        MutableMethodImplementation mutableMethod = new MutableMethodImplementation(implementation);

        for (int currentIndex = startIndex; currentIndex <= endIndex; currentIndex++) {
            MainApp.log("FillWithNops","Replacing instruction \r\n\t" +
                    FormatHelper.instructionToString(mutableMethod.getInstructions().get(currentIndex)) +
                    "\r\n\twith\r\n\t" +
                    FormatHelper.instructionToString(new NopInstruction()));
            mutableMethod.replaceInstruction(currentIndex, new NopInstruction());
        }
        return mutableMethod;
    }

    // replace first instruction (const/16) with nop and second instruction (if-*) with goto
    public static MethodImplementation replaceWithGoto(MethodImplementation implementation, int startIndex) {
        MutableMethodImplementation mutableMethod = new MutableMethodImplementation(implementation);

        NopInstruction mNopInstruction = new NopInstruction();
        MainApp.log("BranchAnalyzer"," Replacing instruction \r\n\t" +
                FormatHelper.instructionToString(mutableMethod.getInstructions().get(startIndex)) +
                "\r\n\twith\r\n\t" +
                FormatHelper.instructionToString(mNopInstruction));
        mutableMethod.replaceInstruction(startIndex, mNopInstruction);

        BuilderInstruction mBranchInstruction = mutableMethod.getInstructions().get(startIndex + 1);

        Label jumpTarget = ((BuilderOffsetInstruction) mBranchInstruction).getTarget();
        GotoInstruction mGoto = new GotoInstruction(jumpTarget);

        MainApp.log("BranchAnalyzer","Replacing instruction \r\n\t" +
                FormatHelper.instructionToString(mutableMethod.getInstructions().get(startIndex + 1)) +
                "\r\n\twith\r\n\t" +
                FormatHelper.instructionToString(mGoto));

        mutableMethod.replaceInstruction(startIndex + 1, mGoto);

        return mutableMethod;
    }

    public static MethodImplementation rewriteMathCrap2(@Nonnull MethodImplementation implementation, int from, int to, BuilderInstruction21s mNewBranchInstruction) {
        ArrayList<? extends Instruction> mInstructions = Lists.newArrayList(implementation.getInstructions());
        MutableMethodImplementation mutableMethod = new MutableMethodImplementation(implementation);

        for (int instructionIndex = 0; instructionIndex < mInstructions.size(); instructionIndex++) {
            if ((instructionIndex >= from) && (instructionIndex < to)) { //All instructions to delete
                BuilderInstruction nop = new NopInstruction();
                // System.out.println("Rewriting " + FormatHelper.instructionToString(mInstructions.get(instructionIndex)) + " to " + FormatHelper.instructionToString(nop));
                mutableMethod.replaceInstruction(instructionIndex, nop);
            } else if (instructionIndex == to) { //last instruction (relevant for branching)
                // System.out.println("returning new branch instruction: " + FormatHelper.instructionToString(mNewBranchInstruction));
                mutableMethod.replaceInstruction(instructionIndex, mNewBranchInstruction);
            }
        }
        return mutableMethod;
    }



    public static boolean opcodeIsBetween(Opcode start, Opcode end, Opcode opcode) {
        return start.ordinal() <= opcode.ordinal() && opcode.ordinal() <= end.ordinal();
    }
}
