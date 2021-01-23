package org.thesis.dexprocessor;

import com.google.common.collect.Lists;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.MultiDexIO;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderTryBlock;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21s;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction;
import org.thesis.ListenerThread;
import org.thesis.MainApp;
import org.thesis.dexprocessor.analyzer.BranchAnalyzer;
import org.thesis.dexprocessor.analyzer.MathAnalyzer;
import org.thesis.dexprocessor.analyzer.SwitchCaseAnalyzer;
import org.thesis.dexprocessor.instructions.GotoInstruction;
import org.thesis.dexprocessor.instructions.NopInstruction;
import org.thesis.dexprocessor.statistics.SanitizerStatistics;
import org.thesis.dexprocessor.vm.SmaliVM;
import org.thesis.dexprocessor.vm.registers.RegisterMap;
import org.thesis.dexprocessor.writeback.FileWriter;
import org.thesis.dexprocessor.writeback.Rewriter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.jf.dexlib2.Opcode.CONST_16;
import static org.jf.dexlib2.Opcode.NOP;

public class Deobfuscator extends ListenerThread {

    private final List<ClassDef> mDeobfuscatedClassDefList;
    private ClassDef mClass = null;
    private int indexInDexFile = -1;
    private boolean multiPass = false;
    private int numberOfPasses = 1;
    private boolean removeAllTryBlocks = true;
    private boolean mathSanitizerEnabled = true;
    private boolean branchSanitizerEnabled = true;
    private boolean switchCaseSanitizerEnabled = true;
    private boolean removeReplacedNops = false;
    private List<SanitizerStatistics> mStatistics = Lists.newArrayList();

    public void setMathSanitizerEnabled(boolean mathSanitizerEnabled) {
        this.mathSanitizerEnabled = mathSanitizerEnabled;
    }

    public void setBranchSanitizerEnabled(boolean branchSanitizerEnabled) {
        this.branchSanitizerEnabled = branchSanitizerEnabled;
    }

    public void setSwitchCaseSanitizerEnabled(boolean switchCaseSanitizerEnabled) {
        this.switchCaseSanitizerEnabled = switchCaseSanitizerEnabled;
    }

    public void setRemoveReplacedNops(boolean removeReplacedNops) {
        this.removeReplacedNops = removeReplacedNops;
    }

    public List<SanitizerStatistics> getStatistics() {
        return mStatistics;
    }


    public Deobfuscator(DexFile mDexFile, String className, boolean multiPass, int numberOfPasses) {
        this(mDexFile);

        this.multiPass = multiPass;
        mClass = getClassDefForName(className);
        this.numberOfPasses = numberOfPasses;
        indexInDexFile = mDeobfuscatedClassDefList.indexOf(mClass);
    }

    public Deobfuscator(DexFile mDexFile) {
        mDeobfuscatedClassDefList = Lists.newArrayList(mDexFile.getClasses());
    }

    private ClassDef getClassDefForName(String className) {
        Optional<? extends ClassDef> mOptionalClassDef = mDeobfuscatedClassDefList.stream().filter(x -> x.getType().equals(className)).findFirst();
        if (mOptionalClassDef.isPresent()) {
            return mOptionalClassDef.get();
        } else {
            throw new RuntimeException("Could not find class: " + className);
        }
    }

    private ClassDef deobfuscateClass(ClassDef mClass, int indexInDexFile) {
        for (Method originalMethod : mClass.getMethods()) {
            MainApp.log("Deobfuscator","Starting deobfuscation of " + originalMethod.getName() + "(" + originalMethod.getParameters() + ")");
            if (originalMethod.getImplementation() != null) {
                try {
                    mClass = Rewriter.rewriteClassDef(mClass, deobfuscateMethod(mClass, originalMethod));
                    MainApp.log("Deobfuscator","Deobfuscated method " + originalMethod.getName() + " of class " + mClass.getType());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        mDeobfuscatedClassDefList.set(indexInDexFile, mClass);
        return mClass;
    }

    private void deobfuscateAllClasses() {
        for (int i = 0; i < mDeobfuscatedClassDefList.size(); i++) {
            ClassDef myClassDef = mDeobfuscatedClassDefList.get(i);
            myClassDef = deobfuscateClass(myClassDef, i);
            mDeobfuscatedClassDefList.set(i, myClassDef);
        }

    }

    private void multiPassDeobfuscation(ClassDef mClass, int indexInDexFile) {
        try {
            MainApp.log("MultiPassDeobfuscator","Pass 1/" + numberOfPasses);
            deobfuscateClass(mClass, indexInDexFile);

            for (int i = 2; i <= numberOfPasses; i++) {
                MainApp.log("MultiPassDeobfuscator", "Pass " + (i) + "/" + numberOfPasses);
                File tempDex = File.createTempFile("tmp",".dex");
                FileWriter mWriter = new FileWriter(this.mDeobfuscatedClassDefList, tempDex);
                mWriter.start();
                mWriter.join();

                DexFile mSecondPassDexFile = MultiDexIO.readDexFile(true, tempDex, new BasicDexFileNamer(), null, null);
                this.mDeobfuscatedClassDefList.clear();
                this.mDeobfuscatedClassDefList.addAll(mSecondPassDexFile.getClasses());

                deobfuscateClass(mClass, indexInDexFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeToFile(File folder, ThreadCompleteListener mThreadCompleteListener) {
        FileWriter mWriter = new FileWriter(mDeobfuscatedClassDefList, folder);
        mWriter.addListener(mThreadCompleteListener);
        mWriter.start();
    }

    private SanitizerStatistics getStatisticForMethod(Method method) {
        for (SanitizerStatistics mStatistic : this.mStatistics) {
            if (mStatistic.methodName.equals(method.getName())
                && mStatistic.mParameterTypes.equals(method.getParameterTypes())
                && mStatistic.methodReturnType.equals(method.getReturnType())) {
                return mStatistic;
            }
        }
        return null;
    }

    public void addBranchSanitizerStatistics(Method currentMethod, int branchesSanitized) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.mBranchStatistics.optimizedBranches = branchesSanitized;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.mBranchStatistics.optimizedBranches = branchesSanitized;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }

    public void addMathSanitizerStatistics(Method currentMethod, int mathCalculated, int mathUncalculated) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.mMathStatistics.optimizedJumps = mathCalculated;
            mStatistics.mMathStatistics.unoptimizedJumps = mathUncalculated;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.mMathStatistics.optimizedJumps = mathCalculated;
            mStatistics.mMathStatistics.unoptimizedJumps = mathUncalculated;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }

    public void addSwitchSanitizerStatistics(Method currentMethod, int branchesSanitized, int branchesUnsanitized) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.mSwitchCaseStatistics.optimizedSwitchCases = branchesSanitized;
            mStatistics.mSwitchCaseStatistics.unoptimizedSwitchCases = branchesUnsanitized;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.mSwitchCaseStatistics.optimizedSwitchCases = branchesSanitized;
            mStatistics.mSwitchCaseStatistics.unoptimizedSwitchCases = branchesUnsanitized;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }

    public void addNopSanitizerStatistics(Method currentMethod, int nopsReplaced) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.mNopStatistics.replacedNops = nopsReplaced;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.mNopStatistics.replacedNops = nopsReplaced;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }

    public void addOverallSanitizerStatistics(Method currentMethod, int instructionsBeforeDeobfuscation, int instructionsAfterDeobfuscation) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.instructionsBeforeOptimisation = instructionsBeforeDeobfuscation;
            mStatistics.instructionsAfterOptimisation = instructionsAfterDeobfuscation;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.instructionsBeforeOptimisation = instructionsBeforeDeobfuscation;
            mStatistics.instructionsAfterOptimisation = instructionsAfterDeobfuscation;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }

    public Method deobfuscateMethod(ClassDef mClass, Method mCurrentMethod) {
        if (mCurrentMethod.getImplementation() == null)
            return mCurrentMethod;

        int instructionsBeforeDeobfuscation = Lists.newArrayList(mCurrentMethod.getImplementation().getInstructions()).size();

        if (mCurrentMethod.getImplementation() == null || Lists.newArrayList(mCurrentMethod.getImplementation().getInstructions()).size() == 0)
            return mCurrentMethod;

        MethodImplementation newMethodImplementation = mCurrentMethod.getImplementation();
        if (mathSanitizerEnabled)
            newMethodImplementation = runMathSanitizer(mClass, mCurrentMethod, newMethodImplementation);
        if (branchSanitizerEnabled)
            newMethodImplementation = runBranchSanitizer(mCurrentMethod, newMethodImplementation);
        if (switchCaseSanitizerEnabled)
            newMethodImplementation = runSwitchCaseSanitizer(mClass, mCurrentMethod, newMethodImplementation);
        if (removeReplacedNops)
            newMethodImplementation = removeReplacedNops(mCurrentMethod, newMethodImplementation);


        ArrayList<Instruction> mInstructions = Lists.newArrayList(newMethodImplementation.getInstructions());
        long instructionsAfterDeobfuscation = IntStream.range(0, mInstructions.size()).filter(value -> !mInstructions.get(value).getOpcode().equals(NOP)).count();
        addOverallSanitizerStatistics(mCurrentMethod, instructionsBeforeDeobfuscation, (int) instructionsAfterDeobfuscation);

        List<TryBlock<? extends ExceptionHandler>> mNewTryBlocks = Lists.newArrayList();
        if (!removeAllTryBlocks)
            mNewTryBlocks = getSanitizedTryBlocks(newMethodImplementation);

        return Rewriter.rewriteMethod(mCurrentMethod, newMethodImplementation, mNewTryBlocks);
    }

    private List<TryBlock<? extends ExceptionHandler>> getSanitizedTryBlocks(MethodImplementation methodImplementation) {
        MutableMethodImplementation mutableMethodImplementation = new MutableMethodImplementation(methodImplementation);

        List<BuilderTryBlock> mTryBlocksToRemove = Lists.newArrayList();
        tryBlockLoop:
        for (BuilderTryBlock tryBlock : mutableMethodImplementation.getTryBlocks()) {
            int startAddress = tryBlock.getStartCodeAddress();
            int endAddress = tryBlock.getCodeUnitCount() + startAddress;
            for (int j = startAddress; j <= endAddress; j++) {
                try {
                    Opcode mOpcode = mutableMethodImplementation.getInstructions().get(j).getOpcode();

                    if (mOpcode.canThrow()) {
                        System.out.println("Opcode can throw, keeping...");
                        continue tryBlockLoop;
                    }
                } catch (IndexOutOfBoundsException e) {
                    continue tryBlockLoop;
                }
            }
            System.out.println("No instruction can throw, removing try block...");
            mTryBlocksToRemove.add(tryBlock);
        }

        List<TryBlock<? extends ExceptionHandler>> mSanitzizedTryBlocks = new ArrayList<>(mutableMethodImplementation.getTryBlocks());
        mSanitzizedTryBlocks.removeIf(mTryBlocksToRemove::contains);
        return mSanitzizedTryBlocks;
    }


    private MethodImplementation removeReplacedNops(Method mMethod, MethodImplementation mImplementation) {
        AtomicInteger removedNops = new AtomicInteger();
        MutableMethodImplementation mMutable = new MutableMethodImplementation(mImplementation);

        IntStream.range(0, mMutable.getInstructions().size())
                .filter(value -> mMutable.getInstructions().get(value).getOpcode().equals(Opcode.NOP))
                .boxed()
                .sorted(Collections.reverseOrder())
                .forEach(integer -> {
                    mMutable.removeInstruction(integer);
                    removedNops.getAndIncrement();
                });

        addNopSanitizerStatistics(mMethod, removedNops.get());
        return mMutable;
    }

    private MethodImplementation runBranchSanitizer(Method mMethod, MethodImplementation mImplementation) {
        int branchesSanitized = 0;
        MethodImplementation newMethodImplementation = mImplementation;

        assert newMethodImplementation != null;
        BranchAnalyzer mBranchAnalyzer = new BranchAnalyzer(newMethodImplementation);

        while (mBranchAnalyzer.hasNext()) {
            BranchAnalyzer.BranchCrap mBranchCrap = mBranchAnalyzer.next();
            MainApp.log("BranchAnalyzer","Found " + mBranchCrap.mOpcode.toString() + " " +
                    "with a previous [const v"+ mBranchCrap.branchRegister + ", #" + mBranchCrap.literal + "] will branch: " + mBranchCrap.willBranch);
            // if branches never, remove branch
            if (mBranchCrap.willBranch) {
                newMethodImplementation = Rewriter.replaceWithGoto(newMethodImplementation, mBranchCrap.instructionIndex);
            } else {
                newMethodImplementation = Rewriter.fillWithNops(newMethodImplementation, mBranchCrap.instructionIndex, mBranchCrap.instructionIndex + 1);
            }
            branchesSanitized++;
        }
        addBranchSanitizerStatistics(mMethod, branchesSanitized);
        return newMethodImplementation;
    }

    private MethodImplementation runMathSanitizer(ClassDef mClassDef, Method mMethod, MethodImplementation mImplementation) {
        int jumpsOptimized = 0;
        int jumpsUnoptimized = 0;

        MathAnalyzer mAnalyzer = new MathAnalyzer(mImplementation);
        MethodImplementation newMethodImplementation = mImplementation;
        MainApp.log("MathAnalyzer","Found " + mAnalyzer.size() + " potential mathematical control flow obfuscations");
        while (mAnalyzer.hasNext()) {
            MathAnalyzer.InstructionCrap mCrap = mAnalyzer.next();

            MainApp.log("MathAnalyzer","Processing instructions from " + mCrap.startIdx + " to " + mCrap.endIdx);
            try {
                RegisterMap mRegisters = SmaliVM.getInstance(mClassDef, mMethod).run(mCrap.startIdx, mCrap.endIdx);

                int relevantJumpRegister = ((TwoRegisterInstruction) mCrap.mInstructions.get(mCrap.mInstructions.size() - 1)).getRegisterA();
                int relevantRegisterValue = mRegisters.get(relevantJumpRegister).getPrimitiveValue();

                BuilderInstruction21s mNewBranchConditionInstruction = new BuilderInstruction21s(CONST_16, relevantJumpRegister, relevantRegisterValue);

                MainApp.log("MathAnalyzer","Modifying branch condition to: " +
                        "\r\n\t" + FormatHelper.instructionToString(mNewBranchConditionInstruction));

                newMethodImplementation = Rewriter.rewriteMathCrap2(newMethodImplementation, mCrap.startIdx, mCrap.endIdx - 1, mNewBranchConditionInstruction);

                jumpsOptimized++;
            } catch (Exception e) {
                MainApp.log("MathAnalyzer",  e.getMessage());
                jumpsUnoptimized++;
            }
        }
        addMathSanitizerStatistics(mMethod, jumpsOptimized, jumpsUnoptimized);
        return newMethodImplementation;
    }

    private MethodImplementation runSwitchCaseSanitizer(ClassDef mClass, Method mMethod, MethodImplementation mMethodImplementation) {
        int switchCasesOptimized = 0;

        MutableMethodImplementation mMutableMethod = new MutableMethodImplementation(mMethodImplementation);

        SwitchCaseAnalyzer mSwitchCaseAnalyzer = new SwitchCaseAnalyzer(mClass, mMethod, mMutableMethod);
        for (SwitchCaseAnalyzer.SwitchCaseCrap switchCaseCrap : mSwitchCaseAnalyzer.getSwitchCaseCrap()) {
            if (switchCaseCrap.willBranch) {
                mMutableMethod.replaceInstruction(switchCaseCrap.indexOfSwitchInstruction, new GotoInstruction(switchCaseCrap.gotoAddress));
                MainApp.log("SwitchCaseSanitizer","Replaced packed-switch instruction at index " + switchCaseCrap.indexOfSwitchInstruction + " with goto to " + switchCaseCrap.gotoAddress.getCodeAddress());
            } else {
                mMutableMethod.replaceInstruction(switchCaseCrap.indexOfSwitchInstruction, new NopInstruction());
                MainApp.log("SwitchCaseSanitizer","Replaced packed-switch instruction at index " + switchCaseCrap.indexOfSwitchInstruction + " with nop");
            }
            switchCasesOptimized++;
        }

        int switchCasesUnoptimized = mSwitchCaseAnalyzer.unoptimizedSwitchCases;
        addSwitchSanitizerStatistics(mMethod, switchCasesOptimized, switchCasesUnoptimized);
        return mMutableMethod;
    }

    @Override
    public void run() {
        if (multiPass) multiPassDeobfuscation(mClass, indexInDexFile);
        else if (numberOfPasses > 1)deobfuscateClass(mClass, indexInDexFile);
        else deobfuscateAllClasses();
        notifyListeners();
    }

    public void setRemoveAllTryBlocks(boolean selected) {
        this.removeAllTryBlocks = selected;
    }
}
