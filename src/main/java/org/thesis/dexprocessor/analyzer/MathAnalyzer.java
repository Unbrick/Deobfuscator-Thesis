package org.thesis.dexprocessor.analyzer;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction21c;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.*;
import org.thesis.dexprocessor.writeback.Rewriter;

import java.util.ArrayList;
import java.util.function.Consumer;

public class MathAnalyzer {

    public class InstructionCrap {
        public int startIdx = 0;
        public int endIdx = 0;
        public ArrayList<? extends Instruction> mInstructions;

        public InstructionCrap(int startIdx, int endIdx, ArrayList<? extends Instruction> mInstructions) {
            this.startIdx = startIdx;
            this.endIdx = endIdx;
            this.mInstructions = mInstructions;
        }
    }

    private ArrayList<InstructionCrap> mMathCraps = new ArrayList<>();
    private int idx = 0;
    private final MethodImplementation mMethodImplementation;

    public MathAnalyzer(MethodImplementation mMethodImplementation) {
        this.mMethodImplementation = mMethodImplementation;
        getPotentialMathCrap();
    }

    public InstructionCrap next() {
        InstructionCrap mCrap = mMathCraps.get(idx);
        idx++;
        return mCrap;
    }

    public int size() {
        return mMathCraps.size();
    }

    public boolean hasNext() {
        return idx < mMathCraps.size();
    }

    private void getPotentialMathCrap() {
        Iterable<? extends Instruction> mInstructions = mMethodImplementation.getInstructions();
        ArrayList<? extends Instruction> mInstructionsList = Lists.newArrayList(mInstructions);

        mInstructionsList.forEach((Consumer<Instruction>) instruction -> {
            if (instruction instanceof DexBackedInstruction21c && instruction.getOpcode().equals(Opcode.SGET)) {
                int firstMathCrap = mInstructionsList.indexOf(instruction);
                for (int currentIndex = firstMathCrap; currentIndex < mInstructionsList.size(); currentIndex++) {
                    Instruction mInstr = mInstructionsList.get(currentIndex);
                    if (mInstr.getOpcode().equals(Opcode.SPUT)
                            && isRemInt(mInstructionsList.get(currentIndex + 1).getOpcode())
                            && isBranchStatement(mInstructionsList.get(currentIndex + 2).getOpcode())) {
                        int lastMathCrap = currentIndex + 2;
                        InstructionCrap mInstructionCrap = new InstructionCrap(firstMathCrap, lastMathCrap, Lists.newArrayList(mInstructionsList.subList(firstMathCrap, currentIndex + 2)));
                        mMathCraps.add(mInstructionCrap);
                        break;
                    }
                }
            }
        });
    }

    private boolean isRemInt(Opcode mOpcode) {
        return mOpcode.equals(Opcode.REM_INT) || mOpcode.equals(Opcode.REM_INT_2ADDR) || mOpcode.equals(Opcode.REM_INT_LIT8) || mOpcode.equals(Opcode.REM_INT_LIT16);
    }

    private boolean isBranchStatement(Opcode mOpcode) {
        return Rewriter.opcodeIsBetween(Opcode.IF_EQ, Opcode.IF_LEZ, mOpcode);
    }
}
