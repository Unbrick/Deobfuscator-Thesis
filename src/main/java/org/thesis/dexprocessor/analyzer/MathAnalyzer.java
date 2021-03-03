package org.thesis.dexprocessor.analyzer;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.thesis.dexprocessor.writeback.Rewriter;

import java.util.ArrayList;
import java.util.function.Consumer;

public class MathAnalyzer {

    public class InstructionCrap {
        public int startIdx = 0;
        public int endIdx = 0;
        public int jumpRegister = -1;
        public ArrayList<? extends BuilderInstruction> mInstructions;

        public InstructionCrap(int startIdx, int endIdx, int jumpRegister,  ArrayList<? extends BuilderInstruction> mInstructions) {
            this.startIdx = startIdx;
            this.endIdx = endIdx;
            this.jumpRegister = jumpRegister;
            this.mInstructions = mInstructions;
        }
    }

    private ArrayList<InstructionCrap> mMathCraps = new ArrayList<>();
    private int idx = 0;
    private final MutableMethodImplementation mMethodImplementation;

    public MathAnalyzer(MutableMethodImplementation mMethodImplementation) {
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
        ArrayList<? extends BuilderInstruction> mInstructionsList = Lists.newArrayList(mMethodImplementation.getInstructions());

        mInstructionsList.forEach((Consumer<BuilderInstruction>) instruction -> {
            if (instruction instanceof BuilderInstruction21c && instruction.getOpcode().equals(Opcode.SGET)) {
                int firstMathCrap = mInstructionsList.indexOf(instruction);
                for (int currentIndex = firstMathCrap; currentIndex < mInstructionsList.size(); currentIndex++) {
                    BuilderInstruction mInstr = mInstructionsList.get(currentIndex);
                    if (mInstr.getOpcode().equals(Opcode.SPUT)
                            && isRemInt(mInstructionsList.get(currentIndex + 1).getOpcode())
                            && isBranchStatement(mInstructionsList.get(currentIndex + 2).getOpcode())) {

                        // if the current instruction is the sput, it is followed by a rem-int and a if-*, both are parts of the math obfuscation
                        int lastMathCrap = currentIndex + 2;

                        // create a sub-list of the instructions
                        ArrayList<BuilderInstruction> mRelevantInstructions = Lists.newArrayList(mInstructionsList.subList(firstMathCrap, currentIndex + 2));

                        // determine the relevant jump register
                        int relevantJumpRegister = ((OneRegisterInstruction) mRelevantInstructions.get(mRelevantInstructions.size() - 1)).getRegisterA();

                        InstructionCrap mInstructionCrap = new InstructionCrap(firstMathCrap, lastMathCrap, relevantJumpRegister, mRelevantInstructions );
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
