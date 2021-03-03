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
import org.thesis.Logger;
import org.thesis.dexprocessor.exceptions.*;
import org.thesis.dexprocessor.vm.SmaliVM;
import org.thesis.dexprocessor.vm.registers.RegisterMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.jf.dexlib2.Opcode.PACKED_SWITCH;
import static org.thesis.dexprocessor.vm.registers.RegisterState.*;

public class SwitchCaseAnalyzer {

    private final ArrayList<SwitchCaseCrap> mCrapList = Lists.newArrayList();
    private final boolean swichCaseUnsafe;
    private final boolean switchCaseLunatic;

    public SwitchCaseAnalyzer(ClassDef mClass, Method mMethod, MutableMethodImplementation mImplementation, boolean swichCaseUnsafe, boolean switchCaseLunatic) {
        this.swichCaseUnsafe = swichCaseUnsafe;
        this.switchCaseLunatic = switchCaseLunatic;
        analyze(mClass, mMethod, mImplementation);
    }

    public static class SwitchCaseCrap {
        public int indexOfSwitchInstruction;
        public int targetCodeAddress;
        public boolean willBranch;
    }

    private void analyze(ClassDef mClass, Method mMethod, MutableMethodImplementation mMutableMethod) {
        ArrayList<Integer> mUnoptimized = new ArrayList<>();

        instructionLoop: for (int currentInstructionIndex = 0; currentInstructionIndex < mMutableMethod.getInstructions().size(); currentInstructionIndex++) {
            BuilderInstruction mBuilderInstruction = mMutableMethod.getInstructions().get(currentInstructionIndex);

            // check whether instruction is of type packed-switch
            if (mBuilderInstruction instanceof BuilderInstruction31t && mBuilderInstruction.getOpcode().equals(PACKED_SWITCH)) {

                BuilderInstruction31t mPackedSwitchInstruction = (BuilderInstruction31t) mBuilderInstruction;

                // get packed switch target label
                Label packedSwitchTargetLabel = mPackedSwitchInstruction.getTarget();

                // get packed switch payload
                BuilderPackedSwitchPayload mPayload = (BuilderPackedSwitchPayload) packedSwitchTargetLabel.getLocation().getInstruction();

                if (mPayload != null // switch payload cannot be null
                        && mPayload.getSwitchElements().size() == 1 // switch elements has a maximum of 1
                       // && mPackedSwitchInstruction.getLocation().getLabels().size() > 0 // there is a label pointing to the current switch-case statement
                         // previous to the switch instruction is a move (could still be unneccessary)
                       // && (Rewriter.opcodeIsBetween(IF_EQ, IF_LEZ, constInstruction.getOpcode()) || Rewriter.opcodeIsBetween(GOTO, GOTO_32, constInstruction.getOpcode())) // the if-* condition previous to the switch payload has been replaced with a const
                    ) {

                    int conditionalRegister = mPackedSwitchInstruction.getRegisterA();

                    RegisterMap mRegisters = null;
                    SmaliVM mSmaliVM = null;

                    try {
                        mSmaliVM = SmaliVM.getInstance(mClass, mMethod, mMutableMethod);
                        mRegisters = mSmaliVM.runWithInconsistentBranchConditionDetection(currentInstructionIndex);
                    } catch (InconsistentBranchConditionException | RegisterNotInitializedException e) {
                        Logger.debug("SwitchCaseAnalyzer", "Branch condition is modified or register is not initialized, skipping optimisation of switch statement " + currentInstructionIndex);
                        // in case no optimisation was found, add to statements which need to be processed further
                        if (swichCaseUnsafe) {
                            mUnoptimized.add(currentInstructionIndex);
                        }
                        continue;
                    } catch(ReachedReturnStatementException e2) {
                        Logger.debug("SwitchCaseAnalyzer", "We reached a return statement without reaching the current instruction, skipping optimisation of switch statement " + currentInstructionIndex);
                        // in case no optimisation was found, add to statements which need to be processed further
                        if (swichCaseUnsafe) {
                            mUnoptimized.add(currentInstructionIndex);
                        }
                        continue;
                    } catch (LoopException e3) {
                        Logger.debug("SwitchCaseAnalyzer", "Ran into a loop starting from " + currentInstructionIndex + ", loop repeates at instruction " + e3.getLoopStartIndex());
                        // in case no optimisation was found, add to statements which need to be processed further
                        if (swichCaseUnsafe) {
                            mUnoptimized.add(currentInstructionIndex);
                        }
                        continue;
                    } catch(MethodNotFoundException e4) {
                        Logger.debug("SwitchCaseAnalyzer", "Method not found, not optimisable: " + e4.getMessage());
                        continue;
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }

                    if (mRegisters.get(conditionalRegister).getState() == UNKNOWN
                            || mRegisters.get(conditionalRegister).getState() ==  UNINITIALIZED
                            || mRegisters.get(conditionalRegister).getState() == OBJECT ) {
                        Logger.debug("SwitchCaseAnalyzer", "Unknown register state, continuting");
                        // in case no optimisation was found, add to statements which need to be processed further
                        if (swichCaseUnsafe) {
                            mUnoptimized.add(currentInstructionIndex);
                        }
                        continue;
                    }

                    int literal = mRegisters.get(conditionalRegister).getPrimitiveValue();

                    if (mPayload.getSwitchElements().size() > 0) {
                        SwitchCaseCrap mCrap = new SwitchCaseCrap();
                        mCrap.indexOfSwitchInstruction = currentInstructionIndex;

                        for (int currentSwitchElementIndex = 0; currentSwitchElementIndex < mPayload.getSwitchElements().size(); currentSwitchElementIndex++) {
                            BuilderSwitchElement currentSwitchElement = mPayload.getSwitchElements().get(currentSwitchElementIndex);
                            int switchKey = currentSwitchElement.getKey();

                            if (switchKey == literal) {
                                mCrap.targetCodeAddress = currentSwitchElement.getTarget().getCodeAddress();
                                mCrap.willBranch = true;
                                this.mCrapList.add(mCrap);

                                continue instructionLoop;
                            }
                        }

                        // before adding the branch condition, verify the VM is not in insecure mode
                        if (!mSmaliVM.isInsecureMode()) {
                            // if branch never taken, switch case will run into default branch
                            mCrap.willBranch = false;
                            this.mCrapList.add(mCrap);
                        }
                    }
                }
            }
        }

        if (swichCaseUnsafe) {
            SwitchCaseAnalyzerTest mTest = new SwitchCaseAnalyzerTest(mMutableMethod, switchCaseLunatic);
            List<SwitchCaseCrap> unsafeCrap = mTest.analyze(mUnoptimized);

            this.mCrapList.addAll(unsafeCrap);
        }
    }

    public ArrayList<SwitchCaseCrap> getSwitchCaseCrap() {
        return mCrapList;
    }
}
