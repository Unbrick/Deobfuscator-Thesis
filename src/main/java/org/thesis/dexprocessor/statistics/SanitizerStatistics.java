package org.thesis.dexprocessor.statistics;

import java.util.List;

public class SanitizerStatistics {

    public String methodName;
    public String methodReturnType;
    public List<? extends CharSequence> mParameterTypes;
    public int instructionsBeforeOptimisation;
    public int instructionsAfterOptimisation;
    public MathSanitizerStatistics mMathStatistics;
    public BranchOptimizerStatistics mBranchStatistics;
    public SwitchCaseOptimizerStatistics mSwitchCaseStatistics;
    public NopReplacements mNopStatistics;

    public SanitizerStatistics(String methodName, String methodReturnType, List<? extends CharSequence> mParameterTypes) {
        this.methodName = methodName;
        this.methodReturnType = methodReturnType;
        this.mParameterTypes = mParameterTypes;
        this.mMathStatistics = new MathSanitizerStatistics();
        this.mBranchStatistics = new BranchOptimizerStatistics();
        this.mSwitchCaseStatistics = new SwitchCaseOptimizerStatistics();
        this.mNopStatistics = new NopReplacements();
    }

    @Override
    public String toString() {
        StringBuilder parameters = new StringBuilder();
        for (CharSequence mParameterType : mParameterTypes) {
            if (mParameterType.length() > 5) {
                String mParameter = (String) mParameterType;
                parameters.append(mParameter.substring(mParameter.lastIndexOf("/") + 1, mParameter.length() - 1));
            } else {
                parameters.append(mParameterType);
            }
            if (!(mParameterTypes.indexOf(mParameterType) == mParameterTypes.size() - 1)) {
                parameters.append(", ");
            }
        }
        return methodName + "(" + parameters.toString() + ")" + methodReturnType.substring(methodReturnType.lastIndexOf("/") + 1);
    }

    public class MathSanitizerStatistics {
        public int optimizedJumps = 0;
        public int unoptimizedJumps = 0;
    }
    public class BranchOptimizerStatistics {
        public int optimizedBranches = 0;
    }
    public class SwitchCaseOptimizerStatistics {
        public int optimizedSwitchCases = 0;
        public int unoptimizedSwitchCases = 0;
    }
    public class NopReplacements {
        public int replacedNops = 0;
    }
}
