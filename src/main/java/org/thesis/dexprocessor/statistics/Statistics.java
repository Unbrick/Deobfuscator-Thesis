package org.thesis.dexprocessor.statistics;

import com.google.common.collect.Lists;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;

import java.util.List;
import java.util.Objects;

public class Statistics {

    public List<SanitizerStatistics> getStatistics() {
        return mStatistics;
    }

    private final List<SanitizerStatistics> mStatistics = Lists.newArrayList();

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

    public long startBranchSanitierTimer(ClassDef mClass, Method mMethod) {
        SanitizerStatistics _stat = getStatisticForMethod(mMethod);
        if (_stat == null) {
            _stat = new SanitizerStatistics(getClassSimpleName(mClass), mMethod.getName(), mMethod.getReturnType(), mMethod.getParameterTypes());
            this.mStatistics.add(_stat);
        }
        return _stat.mBranchStatistics.start();
    }

    private String getClassSimpleName(ClassDef mClassDef) {
        String fullClassDef = mClassDef.getType();
        int lastSlash = mClassDef.getType().lastIndexOf("/");
        return fullClassDef.substring(lastSlash + 1, fullClassDef.length() - 1);
    }

    public long stopBranchSanitizerTimer(Method mMethod) {
        return Objects.requireNonNull(getStatisticForMethod(mMethod)).mBranchStatistics.stop();
    }

    public long startMathSanitizerTimer(ClassDef mClass, Method mMethod) {
        SanitizerStatistics _stat = getStatisticForMethod(mMethod);
        if (_stat == null) {
            _stat = new SanitizerStatistics(getClassSimpleName(mClass), mMethod.getName(), mMethod.getReturnType(), mMethod.getParameterTypes());
            this.mStatistics.add(_stat);
        }
        return _stat.mMathStatistics.start();
    }

    public long stopMathSanitizerTimer(Method mMethod) {
        return Objects.requireNonNull(getStatisticForMethod(mMethod)).mMathStatistics.stop();
    }


    public long startSwitchCaseSanitizerTimer(ClassDef mClass, Method mMethod) {
        SanitizerStatistics _stat = getStatisticForMethod(mMethod);
        if (_stat == null) {
            _stat = new SanitizerStatistics(getClassSimpleName(mClass), mMethod.getName(), mMethod.getReturnType(), mMethod.getParameterTypes());
            this.mStatistics.add(_stat);
        }
        return _stat.mSwitchCaseStatistics.start();
    }

    public long stopSwitchCaseSanitizerTimer(Method mMethod) {
        return Objects.requireNonNull(getStatisticForMethod(mMethod)).mSwitchCaseStatistics.stop();
    }

    public long startTryCatchSanitizerTimer(ClassDef mClass, Method mMethod) {
        SanitizerStatistics _stat = getStatisticForMethod(mMethod);
        if (_stat == null) {
            _stat = new SanitizerStatistics(getClassSimpleName(mClass), mMethod.getName(), mMethod.getReturnType(), mMethod.getParameterTypes());
            this.mStatistics.add(_stat);
        }
        return _stat.mTryCatchStatistics.start();
    }

    public long stopTryCatchSanitizerTimer(Method mMethod) {
        return Objects.requireNonNull(getStatisticForMethod(mMethod)).mTryCatchStatistics.stop();
    }

    public void addBranchSanitizerStatistics(ClassDef mClass, Method currentMethod, int branchesSanitized, int branchesUnsanitized) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(getClassSimpleName(mClass), currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.mBranchStatistics.optimizedBranches = branchesSanitized;
            mStatistics.mBranchStatistics.unoptimizedBranches = branchesUnsanitized;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.mBranchStatistics.optimizedBranches = branchesSanitized;
            mStatistics.mBranchStatistics.unoptimizedBranches = branchesUnsanitized;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }

    public void addTryCatchSanitizerStatistics(ClassDef mClass, Method currentMethod, int tryCatchesSanitized, int tryCatchesUnsanitized) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(getClassSimpleName(mClass), currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.mTryCatchStatistics.tryRemoved = tryCatchesSanitized;
            mStatistics.mTryCatchStatistics.tryTotal = tryCatchesUnsanitized;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.mTryCatchStatistics.tryRemoved = tryCatchesSanitized;
            mStatistics.mTryCatchStatistics.tryTotal = tryCatchesUnsanitized;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }
    public void addSwitchSanitizerStatistics(ClassDef mClass, Method currentMethod, int branchesSanitized, int branchesUnsanitized) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(getClassSimpleName(mClass), currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.mSwitchCaseStatistics.optimizedSwitchCases = branchesSanitized;
            mStatistics.mSwitchCaseStatistics.unoptimizedSwitchCases = branchesUnsanitized;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.mSwitchCaseStatistics.optimizedSwitchCases = branchesSanitized;
            mStatistics.mSwitchCaseStatistics.unoptimizedSwitchCases = branchesUnsanitized;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }

    public void addMathSanitizerStatistics(ClassDef mClass, Method currentMethod, int mathCalculated, int mathUncalculated) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(getClassSimpleName(mClass), currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.mMathStatistics.optimizedJumps = mathCalculated;
            mStatistics.mMathStatistics.unoptimizedJumps = mathUncalculated;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.mMathStatistics.optimizedJumps = mathCalculated;
            mStatistics.mMathStatistics.unoptimizedJumps = mathUncalculated;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }

    public void addNopSanitizerStatistics(ClassDef mClass, Method currentMethod, int nopsReplaced) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(getClassSimpleName(mClass), currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.mNopStatistics.replacedNops = nopsReplaced;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.mNopStatistics.replacedNops = nopsReplaced;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }

    public void addOverallSanitizerStatistics(ClassDef mClass, Method currentMethod, int instructionsBeforeDeobfuscation, int instructionsAfterDeobfuscation) {
        SanitizerStatistics mStatistics = getStatisticForMethod(currentMethod);
        if (mStatistics == null) {
            mStatistics = new SanitizerStatistics(getClassSimpleName(mClass), currentMethod.getName(), currentMethod.getReturnType(), currentMethod.getParameterTypes());
            mStatistics.instructionsBeforeOptimisation = instructionsBeforeDeobfuscation;
            mStatistics.instructionsAfterOptimisation = instructionsAfterDeobfuscation;
            this.mStatistics.add(mStatistics);
        } else {
            mStatistics.instructionsBeforeOptimisation = instructionsBeforeDeobfuscation;
            mStatistics.instructionsAfterOptimisation = instructionsAfterDeobfuscation;
            this.mStatistics.set(this.mStatistics.indexOf(mStatistics), mStatistics);
        }
    }

    public SanitizerStatistics getTotal() {
        var totalStatistics = new SanitizerStatistics("Total", "Total", "", Lists.newArrayList());
        totalStatistics.mMathStatistics.run_duration = mStatistics.stream().mapToInt(value -> (int) value.mMathStatistics.run_duration).sum();
        totalStatistics.mBranchStatistics.run_duration = mStatistics.stream().mapToInt(value -> (int) value.mBranchStatistics.run_duration).sum();
        totalStatistics.mSwitchCaseStatistics.run_duration = mStatistics.stream().mapToInt(value -> (int) value.mSwitchCaseStatistics.run_duration).sum();
        totalStatistics.mTryCatchStatistics.run_duration = mStatistics.stream().mapToInt(value -> (int) value.mTryCatchStatistics.run_duration).sum();

        totalStatistics.mTryCatchStatistics.tryRemoved = mStatistics.stream().mapToInt(value -> value.mTryCatchStatistics.tryRemoved).sum();
        totalStatistics.mTryCatchStatistics.tryTotal = mStatistics.stream().mapToInt(value -> value.mTryCatchStatistics.tryTotal).sum();
        totalStatistics.instructionsAfterOptimisation = mStatistics.stream().mapToInt(value -> value.instructionsAfterOptimisation).sum();
        totalStatistics.instructionsBeforeOptimisation = mStatistics.stream().mapToInt(value -> value.instructionsBeforeOptimisation).sum();
        totalStatistics.mMathStatistics.optimizedJumps = mStatistics.stream().mapToInt(value -> value.mMathStatistics.optimizedJumps).sum();
        totalStatistics.mMathStatistics.unoptimizedJumps = mStatistics.stream().mapToInt(value -> value.mMathStatistics.unoptimizedJumps).sum();
        totalStatistics.mBranchStatistics.optimizedBranches = mStatistics.stream().mapToInt(value -> value.mBranchStatistics.optimizedBranches).sum();
        totalStatistics.mSwitchCaseStatistics.optimizedSwitchCases = mStatistics.stream().mapToInt(value -> value.mSwitchCaseStatistics.optimizedSwitchCases).sum();
        totalStatistics.mSwitchCaseStatistics.unoptimizedSwitchCases = mStatistics.stream().mapToInt(value -> value.mSwitchCaseStatistics.unoptimizedSwitchCases).sum();
        totalStatistics.mNopStatistics.replacedNops = mStatistics.stream().mapToInt(value -> value.mNopStatistics.replacedNops).sum();
        return totalStatistics;
    }

    @Override
    public String toString() {
        return getTotal().toString();
    }
}
