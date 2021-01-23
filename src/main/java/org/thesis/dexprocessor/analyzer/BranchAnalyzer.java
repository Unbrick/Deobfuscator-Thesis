package org.thesis.dexprocessor.analyzer;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.thesis.dexprocessor.writeback.Rewriter;

import java.util.ArrayList;

public class BranchAnalyzer {

    public static class BranchCrap {
        public int instructionIndex;
        public boolean willBranch;
        public int branchRegister;
        public Opcode mOpcode;
        public int literal;
    }

    private final ArrayList<? extends Instruction> mInstructions;
    private final ArrayList<BranchCrap> mBranchCrap = new ArrayList<>();
    private int currentIndex = 0;

    public BranchAnalyzer(MethodImplementation mImplementation) {
        this.mInstructions = Lists.newArrayList(mImplementation.getInstructions());
        analyzeBranchCrap();
    }

    public boolean hasNext() {
        return currentIndex < mBranchCrap.size();
    }

    public BranchCrap next() {
        BranchCrap crap = mBranchCrap.get(currentIndex);
        currentIndex++;
        return crap;
    }

    private void analyzeBranchCrap() {
        for (int instructionIndex = 0; instructionIndex < mInstructions.size(); instructionIndex++) {
            Instruction currentInstruction = mInstructions.get(instructionIndex);
            Opcode currentOpcode = currentInstruction.getOpcode();

            if ((currentOpcode.equals(Opcode.IF_EQZ) || currentOpcode.equals(Opcode.IF_NEZ)) && (instructionIndex - 1 >= 0)) {
                Instruction previousInstruction = mInstructions.get(instructionIndex - 1);
                Opcode previousOpcode = previousInstruction.getOpcode();

                if (Rewriter.opcodeIsBetween(Opcode.CONST_4, Opcode.CONST, previousOpcode)) {
                    int constRegister = ((OneRegisterInstruction) previousInstruction).getRegisterA();
                    int branchRegister = ((OneRegisterInstruction) currentInstruction).getRegisterA();

                    if (branchRegister == constRegister) {
                        // we found gold!
                        int literal = ((NarrowLiteralInstruction) previousInstruction).getNarrowLiteral();


                        BranchCrap mCrap = evaluateJumpCondition(currentInstruction.getOpcode(), literal, instructionIndex - 1);
                        mCrap.literal = literal;
                        mCrap.mOpcode = currentOpcode;
                        mCrap.branchRegister = branchRegister;
                        mBranchCrap.add(mCrap);
                    }
                }
            }
        }
    }

    private BranchCrap evaluateJumpCondition(Opcode branchOpcode, int literal, int instructionIndex) {
        BranchCrap branchCrap = new BranchCrap();
        branchCrap.instructionIndex = instructionIndex;

        if (branchOpcode.equals(Opcode.IF_EQZ)) {
            branchCrap.willBranch = literal == 0;
        } else if (branchOpcode.equals(Opcode.IF_NEZ)) {
            branchCrap.willBranch = literal != 0;
        } else {
            //unknown opcode, dafuq!!!
            throw new RuntimeException("Fuck.");
        }
        return branchCrap;
    }
}
