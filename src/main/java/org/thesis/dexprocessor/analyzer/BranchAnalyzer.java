package org.thesis.dexprocessor.analyzer;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.thesis.dexprocessor.writeback.Rewriter;

import java.util.ArrayList;
import java.util.function.Consumer;

public class BranchAnalyzer {

    public static class BranchCrap {
        // index of the if-* instruction, prior to this a const/* instruction should live
        public int instructionIndex;
        public boolean willBranch;
        public int branchRegister;
        public Opcode mOpcode;
        public int literal;
    }

    private final ArrayList<? extends Instruction> mInstructions;
    private final ArrayList<BranchCrap> mBranchCrap = new ArrayList<>();
    private int currentIndex = 0;
    private MutableMethodImplementation mMutableMethodImplementation;

    public BranchAnalyzer(MutableMethodImplementation mImplementation) {
        this.mInstructions = Lists.newArrayList(mImplementation.getInstructions());
        this.mMutableMethodImplementation = mImplementation;
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
                // mMutableMethodImplementation.getInstructions().get(instructionIndex).getLocation().getLabels()

                Instruction previousInstruction = mInstructions.get(instructionIndex - 1);
                Opcode previousOpcode = previousInstruction.getOpcode();

                if (Rewriter.opcodeIsBetween(Opcode.CONST_4, Opcode.CONST, previousOpcode)) {
                    int constRegister = ((OneRegisterInstruction) previousInstruction).getRegisterA();
                    int branchRegister = ((OneRegisterInstruction) currentInstruction).getRegisterA();

                    if (branchRegister == constRegister) {
                        // we found gold!
                        //TODO CHECK if a label is pointing to the if-* instruction (could be a different branch path)
                        int literal = ((NarrowLiteralInstruction) previousInstruction).getNarrowLiteral();


                        BranchCrap mCrap = evaluateJumpCondition(currentInstruction.getOpcode(), literal, instructionIndex);
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

        // Only optimize branches which use 0 or 1 as literat to prevent false optimisation
        if (branchOpcode.equals(Opcode.IF_EQZ)) {
            branchCrap.willBranch = literal == 0;
        } else if (branchOpcode.equals(Opcode.IF_NEZ)) {
            branchCrap.willBranch = literal == 1;
        } else {
            //unknown opcode, dafuq!!!
            throw new RuntimeException("Fuck.");
        }
        return branchCrap;
    }
}
