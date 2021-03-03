package org.thesis.dexprocessor.statistics;

import java.util.List;

public class SanitizerStatistics {

    private String className;
    public String methodName;
    public String methodReturnType;
    public List<? extends CharSequence> mParameterTypes;
    public int instructionsBeforeOptimisation;
    public int instructionsAfterOptimisation;
    public MathSanitizerStatistics mMathStatistics;
    public BranchOptimizerStatistics mBranchStatistics;
    public SwitchCaseOptimizerStatistics mSwitchCaseStatistics;
    public NopReplacements mNopStatistics;
    public TryCatchesRemoved mTryCatchStatistics;

    public SanitizerStatistics(String className, String methodName, String methodReturnType, List<? extends CharSequence> mParameterTypes) {
        this.className = className;
        this.methodName = methodName;
        this.methodReturnType = methodReturnType;
        this.mParameterTypes = mParameterTypes;
        this.mMathStatistics = new MathSanitizerStatistics();
        this.mBranchStatistics = new BranchOptimizerStatistics();
        this.mSwitchCaseStatistics = new SwitchCaseOptimizerStatistics();
        this.mNopStatistics = new NopReplacements();
        this.mTryCatchStatistics = new TryCatchesRemoved();
    }

    public String getMethod() {
        return toString();
    }

    public String getInstrBefore() {
        return String.valueOf(instructionsBeforeOptimisation);
    }

    public String getInstrAfter() {
        return String.valueOf(instructionsAfterOptimisation);
    }

    public String getMathOptimized() {
        return String.valueOf(mMathStatistics.optimizedJumps);
    }

    public String getMathUnoptimized() {
        return String.valueOf(mMathStatistics.unoptimizedJumps);
    }

    public String getMathTime() {
        return String.valueOf(mMathStatistics.run_duration);
    }

    public String getBranchOptimized() {
        return String.valueOf(mBranchStatistics.optimizedBranches);
    }

    public String getBranchUnoptimized() {
        return String.valueOf(mBranchStatistics.unoptimizedBranches);
    }

    public String getBranchTime() {
        return String.valueOf(mBranchStatistics.run_duration);
    }

    public String getSwCaseOptimized() {
        return String.valueOf(mSwitchCaseStatistics.optimizedSwitchCases);
    }

    public String getSwCaseUnoptimized() {
        return String.valueOf(mSwitchCaseStatistics.unoptimizedSwitchCases);
    }

    public String getSwCaseTime() {
        return String.valueOf(mSwitchCaseStatistics.run_duration);
    }

    public String getTryOptimized() {
        return String.valueOf(mTryCatchStatistics.tryRemoved);
    }

    public String getTryUnoptimized() {
        return String.valueOf(mTryCatchStatistics.tryTotal);
    }

    public String getTryTime() {
        return String.valueOf(mTryCatchStatistics.run_duration);
    }

    public String getNopsRemoved() {
        return String.valueOf(mNopStatistics.replacedNops);
    }

    public String getNopsTime() {
        return String.valueOf(mNopStatistics.run_duration);
    }

    @Override
    public String toString() {
        StringBuilder parameters = new StringBuilder();
        for (CharSequence mParameterType : mParameterTypes) {
            if (mParameterType.length() > 5) {
                String mParameter = (String) mParameterType;
                parameters.append(mParameter, mParameter.lastIndexOf("/") + 1, mParameter.length() - 1);
            } else {
                parameters.append(mParameterType);
            }
            if (!(mParameterTypes.indexOf(mParameterType) == mParameterTypes.size() - 1)) {
                parameters.append(", ");
            }
        }
        return className + "::" + methodName + "(" + parameters.toString() + ")" + methodReturnType.substring(methodReturnType.lastIndexOf("/") + 1);
    }

    public class MathSanitizerStatistics extends AbstractTimerStatistics {
        public int optimizedJumps = 0;
        public int unoptimizedJumps = 0;

        @Override
        public String toString() {
            return "MathSanitizerStatistics{" +
                    "\r\n\trun_duration=" + run_duration +
                    ", \r\n\toptimizedJumps=" + optimizedJumps +
                    ", \r\n\tunoptimizedJumps=" + unoptimizedJumps +
                    "\r\n\t}";
        }
    }
    public class BranchOptimizerStatistics extends AbstractTimerStatistics {
        public int optimizedBranches = 0;
        public int unoptimizedBranches = 0;

        @Override
        public String toString() {
            return "BranchOptimizerStatistics{" +
                    "\r\n\trun_duration=" + run_duration +
                    ", \r\n\toptimizedBranches=" + optimizedBranches +
                    ", \r\n\tunoptimizedBranches=" + unoptimizedBranches +
                    '}';
        }
    }
    public class SwitchCaseOptimizerStatistics extends AbstractTimerStatistics {
        public int optimizedSwitchCases = 0;
        public int unoptimizedSwitchCases = 0;

        @Override
        public String toString() {
            return "SwitchCaseOptimizerStatistics{" +
                    "\r\n\trun_duration=" + run_duration +
                    ", \r\n\toptimizedSwitchCases=" + optimizedSwitchCases +
                    ", \r\n\tunoptimizedSwitchCases=" + unoptimizedSwitchCases +
                    "\r\n\t}";
        }
    }
    public class NopReplacements extends AbstractTimerStatistics {
        public int replacedNops = 0;

        @Override
        public String toString() {
            return "NopReplacements{" +
                    "\r\n\trun_duration=" + run_duration +
                    ", \r\n\treplacedNops=" + replacedNops +
                    "\r\n\t}";
        }
    }

    public class TryCatchesRemoved extends AbstractTimerStatistics {
        public int tryRemoved = 0;
        public int tryTotal = 0;

        @Override
        public String toString() {
            return "TryCatchesRemoved{" +
                    "\r\n\trun_duration=" + run_duration +
                    ", \r\n" +
                    "\ttryCatchRemoved=" + tryRemoved +
                    ", \r\n\ttryCatchNotRemoved=" + tryTotal +
                    "\r\n\t}";
        }
    }

    public String print() {
        return "SanitizerStatistics{" + "\r\ninstructionsBeforeOptimisation=" + instructionsBeforeOptimisation +
                ", instructionsAfterOptimisation=" + instructionsAfterOptimisation +
                ", mMathStatistics=" + mMathStatistics +
                ", \r\nmBranchStatistics=" + mBranchStatistics +
                ", \r\nmSwitchCaseStatistics=" + mSwitchCaseStatistics +
                ", \r\nmTryBlockSanitizer" + mTryCatchStatistics +
                ", \r\nmNopStatistics=" + mNopStatistics +
                "\r\n}";
    }

    public void setInstructionsBeforeOptimisation(int instructionsBeforeOptimisation) {
        this.instructionsBeforeOptimisation = instructionsBeforeOptimisation;
    }

    public void setInstructionsAfterOptimisation(int instructionsAfterOptimisation) {
        this.instructionsAfterOptimisation = instructionsAfterOptimisation;
    }
}
