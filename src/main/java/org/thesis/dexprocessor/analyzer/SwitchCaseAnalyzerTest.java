package org.thesis.dexprocessor.analyzer;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31t;
import org.jf.dexlib2.builder.instruction.BuilderPackedSwitchPayload;
import org.jf.dexlib2.builder.instruction.BuilderSwitchElement;
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.thesis.Logger;
import org.thesis.dexprocessor.FormatHelper;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SwitchCaseAnalyzerTest {

    private static final Set<Opcode> CONST_OPCODES = Collections.unmodifiableSet(EnumSet.of(Opcode.CONST_16, Opcode.CONST_4, Opcode.CONST));
    private static final Set<Opcode> M_OPCODES = Collections.unmodifiableSet(EnumSet.of(Opcode.GOTO, Opcode.GOTO_16, Opcode.GOTO_32));
    private List<BuilderInstruction> mInstructions;
    private boolean switchCaseLunatic;

    public SwitchCaseAnalyzerTest(MutableMethodImplementation mMutableMethodImplementation, boolean switchCaseLunatic) {
        this.mInstructions = mMutableMethodImplementation.getInstructions();
        this.switchCaseLunatic = switchCaseLunatic;
    }

    public List<SwitchCaseAnalyzer.SwitchCaseCrap> analyze(ArrayList<Integer> mUnoptimized) {
        List<SwitchCaseAnalyzer.SwitchCaseCrap> mCrapList = new ArrayList<>();

        mainLoop: for (int unoptimizedInstructionIndex : mUnoptimized) {
            BuilderInstruction instruction = mInstructions.get(unoptimizedInstructionIndex);
            if (instruction.getOpcode().equals(Opcode.PACKED_SWITCH)) {
                Logger.debug("SwitchCaseAnalyzer (Unsafe)", "Trying to optimize instruction: " + FormatHelper.instructionToString(instruction));
                int conditionalRegister = ((OneRegisterInstruction) instruction).getRegisterA();
                Set<Label> labelSet = instruction.getLocation().getLabels();
                List<Integer> potentialSwitchCaseConditions = Lists.newArrayList();

                // look 5 instructions back
                List<BuilderInstruction> constBeforePackedSwitch = null;

                try {
                    constBeforePackedSwitch = findConstBeforeInstruction(new int[]{unoptimizedInstructionIndex}, conditionalRegister);
                } catch (IndexOutOfBoundsException e) {
                    continue;
                }
                constBeforePackedSwitch.forEach(builderInstruction -> {
                    int possibleLiterals = extractConstLiteral(builderInstruction);
                    potentialSwitchCaseConditions.add(possibleLiterals);
                });

                // start from packed-switch statement
                // find all labels pointing to it
                // look backwards to find possible register assignments

                List<int[]> gotos = findGotosForLabels(labelSet);

                for (int[] mGotoInstructions : gotos) {
                    List<BuilderInstruction> constInstructions = findConstBeforeInstruction(mGotoInstructions, conditionalRegister);
                    for (BuilderInstruction constInstruction : constInstructions) {
                        int possibleLiterals = extractConstLiteral(constInstruction);
                        potentialSwitchCaseConditions.add(possibleLiterals);
                    }
                }

                // time to verify whether our packed switch has one of the literals as branch path
                BuilderInstruction31t packedSwitch = (BuilderInstruction31t) instruction;

                BuilderPackedSwitchPayload mPayload = (BuilderPackedSwitchPayload) packedSwitch.getTarget().getLocation().getInstruction();
                assert mPayload != null;
                for (BuilderSwitchElement switchElement : mPayload.getSwitchElements()) {
                    for (Integer potentialSwitchCaseCondition : potentialSwitchCaseConditions) {
                        if (potentialSwitchCaseCondition == switchElement.getKey()) {
                            // here we should have one potential offset
                            SwitchCaseAnalyzer.SwitchCaseCrap mCrap = new SwitchCaseAnalyzer.SwitchCaseCrap();
                            mCrap.indexOfSwitchInstruction = unoptimizedInstructionIndex;
                            mCrap.targetCodeAddress = switchElement.getTarget().getCodeAddress();
                            mCrap.willBranch = true;
                            mCrapList.add(mCrap);

                            Logger.debug("SwitchCaseAnalyzer (Unsafe)", "Found condition " + potentialSwitchCaseCondition + " for switch key " + switchElement.getKey());

                            continue mainLoop;
                        }
                    }
                    // Reaching this, no matching path for the case condition could be found
                    // we now could assume, the default branch is always taken
                    // this will break code! All packed-switch statements will be optimized, whether it makes sense or not!
                    if (switchCaseLunatic) {
                        SwitchCaseAnalyzer.SwitchCaseCrap mCrap = new SwitchCaseAnalyzer.SwitchCaseCrap();
                        mCrap.indexOfSwitchInstruction = unoptimizedInstructionIndex;
                        mCrap.targetCodeAddress = switchElement.getTarget().getCodeAddress();
                        mCrap.willBranch = false;
                        mCrapList.add(mCrap);
                        Logger.debug("SwitchCaseAnalyzer (Lunatic)", "Found no condition for key " + switchElement.getKey() + ", defining as willBranch=false");
                        continue mainLoop;
                    }
                }
            }
        }

        return mCrapList;
    }

    private List<BuilderInstruction> findConstBeforeInstruction(int[] mGotoInstructions, int conditionalRegister) {
        ArrayList<BuilderInstruction> mConstInstructions = new ArrayList<>();

        for (int mGotoInstruction : mGotoInstructions) {
            try {
                mInstructions.subList(mGotoInstruction - 5, mGotoInstruction).stream()
                        .filter(builderInstruction -> CONST_OPCODES.contains(builderInstruction.getOpcode()))
                        .filter(builderInstruction -> ((OneRegisterInstruction) builderInstruction).getRegisterA() == conditionalRegister)
                        .collect(Collectors.toCollection(() -> mConstInstructions));
            } catch (IndexOutOfBoundsException e) {
                System.err.println("Out of range, continuing...");
                throw e;
            }
        }
        return mConstInstructions;
    }

    private List<int[]> findGotosForLabels(Set<Label> labelSet) {
        List<int[]> mGotoPaths = new ArrayList<>();
        for (Label mLabel : labelSet) {
            int[] test = IntStream.of(0, mInstructions.size() - 1)
                    .filter(value -> M_OPCODES.contains(mInstructions.get(value).getOpcode()))
                    .filter(value -> ((OffsetInstruction) mInstructions.get(value)).getCodeOffset() == mLabel.getCodeAddress())
                    .toArray();

            mGotoPaths.add(test);
        }
        return mGotoPaths;
    }

    private int extractConstLiteral(BuilderInstruction mPotentialConstInstruction) {
        return ((NarrowLiteralInstruction) mPotentialConstInstruction).getNarrowLiteral();
    }
}
