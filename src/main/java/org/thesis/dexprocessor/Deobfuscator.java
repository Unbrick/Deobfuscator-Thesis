package org.thesis.dexprocessor;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.*;
import org.jf.dexlib2.builder.instruction.BuilderInstruction20t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21s;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.thesis.ListenerThread;
import org.thesis.Logger;
import org.thesis.dexprocessor.analyzer.BranchAnalyzer;
import org.thesis.dexprocessor.analyzer.MathAnalyzer;
import org.thesis.dexprocessor.analyzer.SwitchCaseAnalyzer;
import org.thesis.dexprocessor.instructions.GotoInstruction;
import org.thesis.dexprocessor.instructions.NopInstruction;
import org.thesis.dexprocessor.statistics.SanitizerStatistics;
import org.thesis.dexprocessor.statistics.Statistics;
import org.thesis.dexprocessor.vm.SmaliVM;
import org.thesis.dexprocessor.vm.registers.RegisterMap;
import org.thesis.dexprocessor.writeback.ClassDefWriteback;
import org.thesis.dexprocessor.writeback.MethodImplementationWriteback;
import org.thesis.dexprocessor.writeback.MethodWriteback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static org.jf.dexlib2.Opcode.*;
import static org.thesis.dexprocessor.FormatHelper.getClassSimpleName;

public class Deobfuscator extends ListenerThread {

    private final Statistics mStatistics = new Statistics();
    private final boolean isPackage;
    private final boolean removeAllTryBlocks;
    private final boolean mathSanitizerEnabled;
    private final boolean branchSanitizerEnabled;
    private final boolean switchCaseSanitizerEnabled;
    private final boolean removeReplacedNops;
    private ClassDef mCurrentClass = null;
    private boolean mathLogic;
    private boolean swichCaseUnsafe;
    private boolean switchCaseLunatic;
    private String mSelectedPackage = null;
    private DexFileManager mDexFile;

    public Deobfuscator(ClassDef mSelectedClass, boolean mathEnabled, boolean branchEnabled, boolean switchCaseEnabled, boolean tryEnabled, boolean removeNops, boolean mathLogic,boolean swichCaseUnsafe,boolean switchCaseLunatic) {
        mCurrentClass = mSelectedClass;
        isPackage = false;
        this.mDexFile = DexFileManager.load(null);

        this.mathSanitizerEnabled = mathEnabled;
        this.branchSanitizerEnabled = branchEnabled;
        this.switchCaseSanitizerEnabled = switchCaseEnabled;
        this.removeAllTryBlocks = tryEnabled;
        this.removeReplacedNops = removeNops;
        this.mathLogic = mathLogic;
        this.swichCaseUnsafe = swichCaseUnsafe;
        this.switchCaseLunatic = switchCaseLunatic;
    }

    public Deobfuscator(String mSelectedPackage, boolean mathEnabled, boolean branchEnabled, boolean switchCaseEnabled, boolean tryEnabled, boolean removeNops, boolean mathLogic,boolean swichCaseUnsafe,boolean switchCaseLunatic) {
        this.mSelectedPackage = mSelectedPackage;
        isPackage = true;
        this.mDexFile = DexFileManager.load(null);

        this.mathSanitizerEnabled = mathEnabled;
        this.branchSanitizerEnabled = branchEnabled;
        this.switchCaseSanitizerEnabled = switchCaseEnabled;
        this.removeAllTryBlocks = tryEnabled;
        this.removeReplacedNops = removeNops;
        this.mathLogic = mathLogic;
        this.swichCaseUnsafe = swichCaseUnsafe;
        this.switchCaseLunatic = switchCaseLunatic;
    }

    public List<SanitizerStatistics> getStatistics() {
        return mStatistics.getStatistics();
    }

    private ClassDef deobfuscateClass(ClassDef mClass) {

        List<Method> deobfuscatedMethods = Lists.newArrayList();

        for (Method originalMethod : mClass.getMethods()) {
            Logger.debug("Deobfuscator", "Starting deobfuscation of " + getClassSimpleName(mClass) + "::" + originalMethod.getName() + "(" + originalMethod.getParameters() + ")");
            if (originalMethod.getImplementation() != null) {
                try {
                    Method rewrittenMethod = deobfuscateMethod(mClass, originalMethod);
                    deobfuscatedMethods.add(rewrittenMethod);
                    Logger.info("Deobfuscator", "Deobfuscated method " + getClassSimpleName(mClass) + "::" + originalMethod.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        mClass = new DexRewriter(new ClassDefWriteback(deobfuscatedMethods)).getClassDefRewriter().rewrite(mClass);

        mDexFile.setClass(mClass);
        Logger.info("Deobfuscator", "Deobfuscator finished with class " + FormatHelper.getClassSimpleName(mClass));
        return mClass;
    }

    public Method deobfuscateMethod(ClassDef mClass, Method mCurrentMethod) {
        if (mCurrentMethod.getImplementation() == null || Lists.newArrayList(mCurrentMethod.getImplementation().getInstructions()).size() == 0)
            return mCurrentMethod;

        int instructionsBeforeDeobfuscation = Lists.newArrayList(mCurrentMethod.getImplementation().getInstructions()).size();

        List<TryBlock<? extends ExceptionHandler>> mTryBlocks = Lists.newArrayList(mCurrentMethod.getImplementation().getTryBlocks());

        MutableMethodImplementation mMethodImplementation = new MutableMethodImplementation(mCurrentMethod.getImplementation());
        if (removeAllTryBlocks)
            mTryBlocks = getSanitizedTryBlocks(mClass, mCurrentMethod, mMethodImplementation);
        if (mathSanitizerEnabled) {
            if (mathLogic)
                runMathSanitizerLogic(mClass, mCurrentMethod, mMethodImplementation);
            else
                runMathSanitizerEmulation(mClass, mCurrentMethod, mMethodImplementation);
        }
        if (branchSanitizerEnabled) {
            runBranchSanitizer(mClass, mCurrentMethod, mMethodImplementation);
        }
        if (switchCaseSanitizerEnabled) {
            runSwitchCaseSanitizer(mClass, mCurrentMethod, mMethodImplementation, swichCaseUnsafe, switchCaseLunatic);
        }
        if (removeReplacedNops) {
            removeReplacedNops(mClass, mCurrentMethod, mMethodImplementation);
        }

        ArrayList<Instruction> mInstructions = Lists.newArrayList(mMethodImplementation.getInstructions());
        long instructionsAfterDeobfuscation = IntStream.range(0, mInstructions.size()).filter(value -> !mInstructions.get(value).getOpcode().equals(NOP)).count();
        mStatistics.addOverallSanitizerStatistics(mClass, mCurrentMethod, instructionsBeforeDeobfuscation, (int) instructionsAfterDeobfuscation);

        MethodImplementation mImplemetaton = new DexRewriter(new MethodImplementationWriteback(mTryBlocks)).getMethodImplementationRewriter().rewrite(mMethodImplementation);

        return new DexRewriter(new MethodWriteback(mImplemetaton)).getMethodRewriter().rewrite(mCurrentMethod);
        // return Rewriter.rewriteMethod(mCurrentMethod, mMethodImplementation, mTryBlocks);
    }

    private List<TryBlock<? extends ExceptionHandler>> getSanitizedTryBlocks(ClassDef mClass, Method mMethod, MutableMethodImplementation methodImplementation) {
        ArrayList<BuilderInstruction> mInstructions = Lists.newArrayList(methodImplementation.getInstructions());
        ArrayList<TryBlock<? extends ExceptionHandler>> tryBlocksToKeep = Lists.newArrayList();

        mStatistics.startTryCatchSanitizerTimer(mClass, mMethod);

        tryBlockLoop:
        for (BuilderTryBlock tryBlock : methodImplementation.getTryBlocks()) {


            for (int index = tryBlock.start.getLocation().getIndex(); index <= tryBlock.end.getLocation().getIndex(); index++) {
                if (!mInstructions.get(index).getOpcode().canThrow()) {
                    //Logger.debug("TryCatchDeobfuscation", "No opcode in try block " + tryBlock.start.getLocation().getIndex() + "-" + tryBlock.end.getLocation().getIndex() + " can throw, removing try block!");
                    continue tryBlockLoop;
                }
            }

            // create a intermediate list as we cannot modify the exception handlers directly
            ArrayList<BuilderExceptionHandler> exceptionHandlers = Lists.newArrayList(tryBlock.getExceptionHandlers());

            for (BuilderExceptionHandler exceptionHandler : tryBlock.getExceptionHandlers()) {

                // get the first exception handler instruction
                int instructionIndex = exceptionHandler.getHandler().getLocation().getIndex();
                Instruction firstExceptionHandlerInstruction = mInstructions.get(instructionIndex);

                // check whether it is a MOVE_EXCEPTION opcode
                if (firstExceptionHandlerInstruction != null && firstExceptionHandlerInstruction.getOpcode().equals(MOVE_EXCEPTION)) {
                    // if so, get the register used
                    int firstInstructionRegisterNumber = ((OneRegisterInstruction) firstExceptionHandlerInstruction).getRegisterA();

                    Instruction secondExceptionHandlerInstruction = mInstructions.get(instructionIndex + 1);

                    // if the next instruction is a goto, follow it and look for the throw instruction
                    if (secondExceptionHandlerInstruction.getOpcode().equals(GOTO)
                            || secondExceptionHandlerInstruction.getOpcode().equals(GOTO_16)
                            || secondExceptionHandlerInstruction.getOpcode().equals(GOTO_32)) {
                        // lets see where this goto redirects us...
                        int targetIndex = ((BuilderOffsetInstruction) secondExceptionHandlerInstruction).getTarget().getLocation().getIndex();
                        secondExceptionHandlerInstruction = mInstructions.get(targetIndex);
                    }
                    if (!secondExceptionHandlerInstruction.getOpcode().equals(THROW)) {
                        //Logger.debug("TryCatchDeobfuscation", "Invalid instruction found: " + FormatHelper.instructionToString(secondExceptionHandlerInstruction) + " cannot continue with optimisation!");
                        continue tryBlockLoop;
                    }

                    int secondInstructionRegisterNumber = ((OneRegisterInstruction) secondExceptionHandlerInstruction).getRegisterA();

                    // check whether the next instruction is a throw, if so verify the same register is used in the move exception and in the throw
                    if (secondExceptionHandlerInstruction.getOpcode().equals(THROW) && firstInstructionRegisterNumber == secondInstructionRegisterNumber) {
                        // this is the default obfuscated exception handler
                        exceptionHandlers.remove(exceptionHandler);
                        //Logger.debug("TryCatchDeobfuscation", "Removed exception handler of type " + exceptionHandler.getExceptionType() + "at " + exceptionHandler.getHandlerCodeAddress());
                    } else {
                        // this is some weired exception handler, most probably not created by obfuscation
                        //Logger.debug("TryCatchDeobfuscation", "Found unknown instruction: " + FormatHelper.instructionToString(secondExceptionHandlerInstruction));
                    }
                }
            }

            // here the exception handler should be empty if it was only obfuscation, we can therefore omit the try block
            // if the exception handler list is not empty, we need to leave the try block in place
            if (exceptionHandlers.size() > 0) {
                tryBlocksToKeep.add(tryBlock);
            } else {
                Logger.debug("TryCatchDeobfuscation", "No exception handlers for try block " + tryBlock.start.getLocation().getIndex() + "-" + tryBlock.end.getLocation().getIndex() + " are left, removing try block!");
            }
        }

        mStatistics.stopTryCatchSanitizerTimer(mMethod);
        mStatistics.addTryCatchSanitizerStatistics(mClass, mMethod, methodImplementation.getTryBlocks().size() - tryBlocksToKeep.size(), methodImplementation.getTryBlocks().size());
        return tryBlocksToKeep;
    }


    private MutableMethodImplementation removeReplacedNops(ClassDef mClass, Method mMethod, MutableMethodImplementation mMutable) {
        AtomicInteger removedNops = new AtomicInteger();

        IntStream.range(0, mMutable.getInstructions().size())
                .filter(value -> mMutable.getInstructions().get(value).getOpcode().equals(Opcode.NOP))
                .boxed()
                .sorted(Collections.reverseOrder())
                .forEach(integer -> {
                    mMutable.removeInstruction(integer);
                    removedNops.getAndIncrement();
                });

        mStatistics.addNopSanitizerStatistics(mClass, mMethod, removedNops.get());
        return mMutable;
    }

    private MutableMethodImplementation runBranchSanitizer(ClassDef mClass, Method mMethod, MutableMethodImplementation mImplementation) {
        int branchesSanitized = 0;
        int branchesUnsanitized = 0;

        mStatistics.startBranchSanitierTimer(mClass, mMethod);
        BranchAnalyzer mBranchAnalyzer = new BranchAnalyzer(mImplementation);
        while (mBranchAnalyzer.hasNext()) {
            BranchAnalyzer.BranchCrap mBranchCrap = mBranchAnalyzer.next();
            Logger.debug("BranchAnalyzer", "Found " + mBranchCrap.mOpcode.toString() + " " +
                    "with a previous [const v" + mBranchCrap.branchRegister + ", #" + mBranchCrap.literal + "] will branch: " + mBranchCrap.willBranch);
            // if branches never, remove branch
            if (mBranchCrap.willBranch && mImplementation.getInstructions().get(mBranchCrap.instructionIndex) instanceof BuilderOffsetInstruction) {
                // replace the rem-int instruction with a nop and add a second one to cover the size of a rem-int/lit*
                mImplementation.replaceInstruction(mBranchCrap.instructionIndex - 1, new NopInstruction());
                // replace the if-* instruction with a goto/16
                Label jumpTarget = ((BuilderOffsetInstruction) mImplementation.getInstructions().get(mBranchCrap.instructionIndex)).getTarget();
                // if-* are 4 bytes long, replace with a goto/16
                mImplementation.replaceInstruction(mBranchCrap.instructionIndex, new BuilderInstruction20t(GOTO_16, jumpTarget));

                //mutableMethod.addInstruction(mBranchCrap.instructionIndex - 1, new NopInstruction());
            } else {
                mImplementation.replaceInstruction(mBranchCrap.instructionIndex - 1, new NopInstruction());
                mImplementation.replaceInstruction(mBranchCrap.instructionIndex, new NopInstruction());

                //mutableMethod.addInstruction(mBranchCrap.instructionIndex - 1, new NopInstruction());
                //mutableMethod.addInstruction(mBranchCrap.instructionIndex, new NopInstruction());
            }
            branchesSanitized++;
        }
        mStatistics.stopBranchSanitizerTimer(mMethod);
        mStatistics.addBranchSanitizerStatistics(mClass, mMethod, branchesSanitized, branchesUnsanitized);
        return mImplementation;
    }

    private MutableMethodImplementation runMathSanitizerLogic(ClassDef mClassDef, Method mMethod, MutableMethodImplementation mImplementation) {
        int jumpsOptimized = 0;
        int jumpsUnoptimized = 0;

        mStatistics.startMathSanitizerTimer(mClassDef, mMethod);
        MathAnalyzer mAnalyzer = new MathAnalyzer(mImplementation);
        while (mAnalyzer.hasNext()) {
            MathAnalyzer.InstructionCrap mCrap = mAnalyzer.next();

            // lets get the field name of the sget instruction used
            String fieldName = ((FieldReference) ((ReferenceInstruction) mCrap.mInstructions.get(0)).getReference()).getName();

            // retrieve the static constructor of the class
            Optional<? extends Method> mClinit = Lists.newArrayList(mClassDef.getMethods()).stream().filter((Predicate<Method>) method -> method.getName().equals("<clinit>")).findFirst();

            if (mClinit.isPresent() && mClinit.get().getImplementation() != null) {
                // get the instructions of the clinit method
                ArrayList<? extends Instruction> mInstructions = Lists.newArrayList(mClinit.get().getImplementation().getInstructions());

                // locate our sput instruction
                Optional<? extends Instruction> sputInstruction = mInstructions.stream().filter((Predicate<Instruction>) instruction -> instruction.getOpcode().equals(SPUT) && ((FieldReference) ((ReferenceInstruction) instruction).getReference()).getName().equals(fieldName)).findFirst();

                if (sputInstruction.isPresent()) {

                    // find the conditional register
                    int conditionalRegister = ((OneRegisterInstruction) sputInstruction.get()).getRegisterA();

                    // build up a array list of instructions in reversed order to iterate back to the beginning
                    int indexOfSput = mInstructions.indexOf(sputInstruction.get());
                    ArrayList<? extends Instruction> shrinkedInstructionList = Lists.newArrayList(mInstructions.subList(0, indexOfSput));
                    Collections.reverse(shrinkedInstructionList);

                    // search for the first const instruction (in backwards order) which uses the same register as the initial sput
                    Optional<? extends Instruction> mConstInstruction = shrinkedInstructionList.stream()
                            .filter((Predicate<Instruction>) instruction -> instruction.getOpcode().equals(CONST_4)
                                    && ((OneRegisterInstruction) instruction).getRegisterA() == conditionalRegister).findFirst();

                    if (mConstInstruction.isPresent()) {
                        // get the literal of the const instruction
                        int literal = ((NarrowLiteralInstruction) mConstInstruction.get()).getNarrowLiteral();

                        // we found the required literal in the clinit method for the field used in the obfuscation
                        // as of current observations the literal used for intializing is always zero or one
                        if (literal == 0 || literal == 1) {
                            // all we have to do now is invert the literal and write it back as jump condition
                            int branchConditionLiteral = literal ^ 1;
                            BuilderInstruction21s mNewBranchConditionInstruction = new BuilderInstruction21s(CONST_16, mCrap.jumpRegister, branchConditionLiteral);

                            Logger.debug("MathAnalyzer", "Modifying branch condition to: " +
                                    "\r\n\t" + FormatHelper.instructionToString(mNewBranchConditionInstruction));

                            jumpsOptimized++;


                            for (int instructionIndex = mCrap.startIdx; instructionIndex <= mCrap.endIdx - 1; instructionIndex++) {
                                if (instructionIndex == mCrap.endIdx - 1) {
                                    //last instruction (relevant for branching)
                                    mImplementation.replaceInstruction(instructionIndex, mNewBranchConditionInstruction);
                                } else {
                                    //All instructions to delete / overwrite with nop
                                    BuilderInstruction nop = new NopInstruction();
                                    mImplementation.replaceInstruction(instructionIndex, nop);
                                }
                            }
                        } else {
                            Logger.debug("MathSanitizerLogic", "The const instruction assigned a literal different from 0 or 1, the literal is: " + literal);
                            jumpsUnoptimized++;
                        }
                    } else {
                        Logger.debug("MathSanitizerLogic", "Could not locate a const instruction assigning a register for the sput instruction");
                        jumpsUnoptimized++;
                    }
                } else {
                    Logger.debug("MathSanitizerLogic", "Could not locate sput instruction in clinit");
                    jumpsUnoptimized++;
                }
            } else {
                Logger.debug("MathSanitizerLogic", "Could not locate clinit");
                jumpsUnoptimized++;
            }
        }
        mStatistics.stopMathSanitizerTimer(mMethod);
        mStatistics.addMathSanitizerStatistics(mClassDef, mMethod, jumpsOptimized, jumpsUnoptimized);
        return mImplementation;
    }

    private MethodImplementation runMathSanitizerEmulation(ClassDef mClassDef, Method mMethod, MutableMethodImplementation mImplementation) {
        int jumpsOptimized = 0;
        int jumpsUnoptimized = 0;

        mStatistics.startMathSanitizerTimer(mClassDef, mMethod);
        MathAnalyzer mAnalyzer = new MathAnalyzer(mImplementation);
        Logger.debug("MathAnalyzer", "Found " + mAnalyzer.size() + " potential mathematical control flow obfuscations");
        while (mAnalyzer.hasNext()) {
            MathAnalyzer.InstructionCrap mCrap = mAnalyzer.next();

            Logger.debug("MathAnalyzer", "Processing instructions from " + mCrap.startIdx + " to " + mCrap.endIdx);
            try {
                RegisterMap mRegisters = SmaliVM.getInstance(mClassDef, mMethod, mImplementation).run(mCrap.startIdx, mCrap.endIdx);

                // get the relevant jump register from the last instruction of the math obfuscation (usually the rem-int vX, vX, #2)
                int relevantJumpRegister = ((TwoRegisterInstruction) mCrap.mInstructions.get(mCrap.mInstructions.size() - 1)).getRegisterA();

                // create a new const/16 instruction with the jump register of the if-* condition and the parameter of the emulation
                BuilderInstruction21s mNewBranchConditionInstruction = new BuilderInstruction21s(CONST_16, relevantJumpRegister, mRegisters.get(relevantJumpRegister).getPrimitiveValue());
                Logger.debug("MathAnalyzer", "Modifying branch condition to: " + "\r\n\t" + FormatHelper.instructionToString(mNewBranchConditionInstruction));

                for (int instructionIndex = mCrap.startIdx; instructionIndex <= mCrap.endIdx - 1; instructionIndex++) {
                    if (instructionIndex == mCrap.endIdx - 1) {
                        //last instruction (relevant for branching)
                        mImplementation.replaceInstruction(instructionIndex, mNewBranchConditionInstruction);
                    } else {
                        //All instructions to delete / overwrite with nop
                        BuilderInstruction nop = new NopInstruction();
                        mImplementation.replaceInstruction(instructionIndex, nop);
                    }
                }

                jumpsOptimized++;
            } catch (Exception e) {
                Logger.error("MathAnalyzer", e.getMessage());
                jumpsUnoptimized++;
            }
        }
        mStatistics.stopMathSanitizerTimer(mMethod);
        mStatistics.addMathSanitizerStatistics(mClassDef, mMethod, jumpsOptimized, jumpsUnoptimized);
        return mImplementation;
    }

    private MutableMethodImplementation runSwitchCaseSanitizer(ClassDef mClass, Method mMethod, MutableMethodImplementation mMutableMethod, boolean swichCaseUnsafe, boolean switchCaseLunatic) {
        int switchCasesOptimized = 0;


        int totalPackedSwitchStatements = Math.toIntExact(mMutableMethod.getInstructions().stream().filter(builderInstruction -> builderInstruction.getOpcode().equals(PACKED_SWITCH)).count());

        mStatistics.startSwitchCaseSanitizerTimer(mClass, mMethod);
        SwitchCaseAnalyzer mSwitchCaseAnalyzer = new SwitchCaseAnalyzer(mClass, mMethod, mMutableMethod, swichCaseUnsafe, switchCaseLunatic);
        for (SwitchCaseAnalyzer.SwitchCaseCrap switchCaseCrap : mSwitchCaseAnalyzer.getSwitchCaseCrap()) {
            Instruction instructionToReplace = mMutableMethod.getInstructions().get(switchCaseCrap.indexOfSwitchInstruction);
            if (switchCaseCrap.willBranch) {
                // define a new label to branch to
                Label newLabel = mMutableMethod.newLabelForAddress(switchCaseCrap.targetCodeAddress);
                GotoInstruction mGotoInstruction = new GotoInstruction(newLabel);
                mMutableMethod.replaceInstruction(switchCaseCrap.indexOfSwitchInstruction, mGotoInstruction);
                //mMutableMethod.replaceInstruction(switchCaseCrap.indexOfSwitchInstruction, new GotoInstruction(switchCaseCrap.gotoAddress));
                //Logger.debug("SwitchCaseSanitizer", "Replaced \r\n\t" + FormatHelper.instructionToString(instructionToReplace) + "\r\n\tat " + switchCaseCrap.indexOfSwitchInstruction + " with\r\n\t " + FormatHelper.instructionToString(mGotoInstruction));
            } else {
                // to fill up the space three nop instructions are required
                //NopInstruction mNopInstruction = new NopInstruction();
                //mMutableMethod.replaceInstruction(switchCaseCrap.indexOfSwitchInstruction, mNopInstruction);
                //mMutableMethod.addInstruction(switchCaseCrap.indexOfSwitchInstruction, mNopInstruction);
                //mMutableMethod.addInstruction(switchCaseCrap.indexOfSwitchInstruction, mNopInstruction);
                //mMutableMethod.replaceInstruction(switchCaseCrap.indexOfSwitchInstruction, new NopInstruction());
                Label newLabel = mMutableMethod.newLabelForIndex(switchCaseCrap.indexOfSwitchInstruction + 1);
                GotoInstruction mGotoInstruction = new GotoInstruction(newLabel);
                mMutableMethod.replaceInstruction(switchCaseCrap.indexOfSwitchInstruction, mGotoInstruction);
                Logger.debug("SwitchCaseSanitizer", "Replaced \r\n\t" + FormatHelper.instructionToString(instructionToReplace) + "\r\n\tat " + switchCaseCrap.indexOfSwitchInstruction + " with\r\n\t " + FormatHelper.instructionToString(mGotoInstruction));
            }
            switchCasesOptimized++;
        }

        mStatistics.stopSwitchCaseSanitizerTimer(mMethod);
        int switchCasesUnoptimized =  totalPackedSwitchStatements - switchCasesOptimized;
        mStatistics.addSwitchSanitizerStatistics(mClass, mMethod, switchCasesOptimized, switchCasesUnoptimized);
        return mMutableMethod;
    }

    @Override
    public void run() {
        List<String> mDeobfuscatedClasses = new ArrayList<>();
        if (!isPackage) {
            // deobfuscate all subclasses
            // int _indexInDexFile = mDexFile.getClassList().indexOf(classDef);
            mDexFile.getClassList().stream().filter(classDef -> {
                String rawClassType = mCurrentClass.getType().substring(0, mCurrentClass.length() - 1);
                return classDef.getType().contains(rawClassType);
            })
                    .peek(this::deobfuscateClass)
                    .forEach(classDef -> mDeobfuscatedClasses.add(FormatHelper.getClassSimpleName(classDef)));
            deobfuscateClass(mCurrentClass);
        } else {
            mDexFile.getClassList().stream().filter(classDef -> classDef.getType().contains(mSelectedPackage)).forEach(classDef -> {
                mDexFile.setClass(deobfuscateClass(classDef));
                mDeobfuscatedClasses.add(FormatHelper.getClassSimpleName(classDef));
            });
        }
        Logger.info("Deobfuscator", "Deobfuscator finished! Classes processed: " + mDeobfuscatedClasses);

        notifyListeners();
    }
}
