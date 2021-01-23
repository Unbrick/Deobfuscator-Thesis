package org.thesis.dexprocessor.analyzer;

import com.google.common.collect.Lists;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31t;
import org.jf.dexlib2.builder.instruction.BuilderPackedSwitchPayload;
import org.jf.dexlib2.builder.instruction.BuilderSwitchElement;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.thesis.MainApp;
import org.thesis.dexprocessor.exceptions.InconsistentBranchConditionException;
import org.thesis.dexprocessor.exceptions.LoopException;
import org.thesis.dexprocessor.exceptions.ReachedReturnStatementException;
import org.thesis.dexprocessor.exceptions.RegisterNotInitializedException;
import org.thesis.dexprocessor.vm.SmaliVM;
import org.thesis.dexprocessor.vm.registers.RegisterMap;
import org.thesis.dexprocessor.writeback.Rewriter;

import java.util.ArrayList;
import java.util.Objects;

import static org.jf.dexlib2.Opcode.*;

public class SwitchCaseAnalyzer {

    private ArrayList<SwitchCaseCrap> mCrapList = Lists.newArrayList();
    public int unoptimizedSwitchCases = 0;

    public SwitchCaseAnalyzer(ClassDef mClass, Method mMethod, MethodImplementation mImplementation) {
        analyze(mClass, mMethod, mImplementation);
    }

    public class SwitchCaseCrap {
        public int indexOfSwitchInstruction;
        public Label gotoAddress;
        public boolean willBranch;
    }

    private void analyze(ClassDef mClass, Method mMethod, MethodImplementation mImplementation) {
        MutableMethodImplementation mMutableMethod = new MutableMethodImplementation(Objects.requireNonNull(mImplementation));

        instructionLoop: for (int currentInstructionIndex = 0; currentInstructionIndex < mMutableMethod.getInstructions().size(); currentInstructionIndex++) {
            BuilderInstruction mBuilderInstruction = mMutableMethod.getInstructions().get(currentInstructionIndex);

            // check whether instruction is of type packed-switch
            if (mBuilderInstruction instanceof BuilderInstruction31t && mBuilderInstruction.getOpcode().equals(PACKED_SWITCH)) {

                BuilderInstruction31t mPackedSwitchInstruction = (BuilderInstruction31t) mBuilderInstruction;
                BuilderInstruction moveInstruction = mMutableMethod.getInstructions().get(currentInstructionIndex - 1);
                BuilderInstruction constInstruction = mMutableMethod.getInstructions().get(currentInstructionIndex - 2);

                // get packed switch target label
                Label packedSwitchTargetLabel = mPackedSwitchInstruction.getTarget();

                // get packed switch payload
                BuilderPackedSwitchPayload mPayload = (BuilderPackedSwitchPayload) packedSwitchTargetLabel.getLocation().getInstruction();

                if (mPayload != null // switch payload cannot be null
                        && mPayload.getSwitchElements().size() == 1 // switch elements has a maximum of 1
                        && mPackedSwitchInstruction.getLocation().getLabels().size() > 0 // there is a label pointing to the current switch-case statement
                         // previous to the switch instruction is a move (could still be unneccessary)
                        && (Rewriter.opcodeIsBetween(IF_EQ, IF_LEZ, constInstruction.getOpcode()) || Rewriter.opcodeIsBetween(GOTO, GOTO_32, constInstruction.getOpcode())) // the if-* condition previous to the switch payload has been replaced with a const
                    ) {

                    int conditionalRegister = mPackedSwitchInstruction.getRegisterA();

                    RegisterMap mRegisters = null;

                    try {
                        mRegisters = SmaliVM
                                .getInstance(mClass, mMethod)
                                .runWithInconsistentBranchConditionDetection(currentInstructionIndex, conditionalRegister);
                    } catch (InconsistentBranchConditionException | RegisterNotInitializedException e) {
                        MainApp.log("SwitchCaseAnalyzer", "Branch condition is modified or register is not initialized, skipping optimisation of switch statement " + currentInstructionIndex);
                        unoptimizedSwitchCases++;
                        continue;
                    } catch(ReachedReturnStatementException e2) {
                        MainApp.log("SwitchCaseAnalyzer", "We reached a return statement without reaching the current instruction, skipping optimisation of switch statement " + currentInstructionIndex);
                        unoptimizedSwitchCases++;
                        continue;
                    } catch (LoopException e3) {
                        MainApp.log("SwitchCaseAnalyzer", "Ran into a loop starting from " + currentInstructionIndex + ", loop repeates at instruction " + e3.getLoopStartIndex());
                        unoptimizedSwitchCases++;
                        continue;
                    }

                    int literal = mRegisters.get(conditionalRegister).getPrimitiveValue();

                    if (mPayload.getSwitchElements().size() > 0) {
                        for (int currentSwitchElementIndex = 0; currentSwitchElementIndex < mPayload.getSwitchElements().size(); currentSwitchElementIndex++) {
                            BuilderSwitchElement currentSwitchElement = mPayload.getSwitchElements().get(currentSwitchElementIndex);
                            int switchKey = currentSwitchElement.getKey();

                            if (switchKey == literal) {
                                SwitchCaseCrap mCrap = new SwitchCaseCrap();
                                mCrap.gotoAddress = currentSwitchElement.getTarget();
                                mCrap.willBranch = true;
                                mCrap.indexOfSwitchInstruction = currentInstructionIndex;
                                this.mCrapList.add(mCrap);

                                continue instructionLoop;
                            }
                        }
                        SwitchCaseCrap mCrap = new SwitchCaseCrap();
                        mCrap.indexOfSwitchInstruction = currentInstructionIndex;
                        mCrap.willBranch = false;
                        this.mCrapList.add(mCrap);
                        // if branch never taken, switch case will run into default branch
                    }
                }
            }
        }
    }

    public ArrayList<SwitchCaseCrap> getSwitchCaseCrap() {
        return mCrapList;
    }
}
