package org.thesis.dexprocessor.vm;

import com.google.common.collect.Lists;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.BuilderOffsetInstruction;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31t;
import org.jf.dexlib2.builder.instruction.BuilderPackedSwitchPayload;
import org.jf.dexlib2.builder.instruction.BuilderSwitchElement;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.thesis.MainApp;
import org.thesis.dexprocessor.exceptions.InconsistentBranchConditionException;
import org.thesis.dexprocessor.exceptions.LoopException;
import org.thesis.dexprocessor.exceptions.ReachedReturnStatementException;
import org.thesis.dexprocessor.exceptions.RegisterNotInitializedException;
import org.thesis.dexprocessor.vm.instancefields.FieldMap;
import org.thesis.dexprocessor.vm.registers.RegisterMap;
import org.thesis.dexprocessor.vm.registers.RegisterState;

import java.util.ArrayList;
import java.util.List;

public class SmaliVM {

    private static SmaliVM instance;
    public int currentInstructionIndex = 0;
    public boolean ignoreLoopException = false;
    public boolean ignoreReturnStatementException = false;
    public List<Integer> jumpAddressList = new ArrayList<>();

    public RegisterMap mRegisters;
    public FieldMap mFields;
    public MethodManager mMethodManager;

    public static SmaliVM getInstance(ClassDef mClass, Method mMethod) {
        if (instance == null)
            instance = new SmaliVM(mClass, mMethod);

        if (!mMethod.getName().equals("<clinit>")) {
            instance.currentInstructionIndex = 0;
            instance.jumpAddressList = Lists.newArrayList();
            instance.mFields.clearAndInit();
            instance.executeClinit();
        }

        instance.mMethodManager.selectMethod(mMethod);
        instance.currentInstructionIndex = 0;
        instance.jumpAddressList = Lists.newArrayList();
        instance.mRegisters.clearAndInit();

        return instance;
    }

    private SmaliVM(ClassDef mClass, Method mMethod) {
        this.currentInstructionIndex = 0;
        this.mRegisters = new RegisterMap(50);
        this.mFields = new FieldMap(mClass.getFields());
        this.mMethodManager = new MethodManager(mClass, mMethod);
    }

    public RegisterMap runWithInconsistentBranchConditionDetection(int stopAtIndex, int conditionalRegister) {
        RegisterMap mLastVM = this.run(0, stopAtIndex);
        try {
            ignoreLoopException = true;
            RegisterMap mCurrentVM = this.run(currentInstructionIndex, currentInstructionIndex - 1);
            if (mLastVM != null && mCurrentVM != null) {
                int lastVMRegister = mLastVM.get(conditionalRegister).getPrimitiveValue();
                int currentVMRegister = mCurrentVM.get(conditionalRegister).getPrimitiveValue();
                if (lastVMRegister != currentVMRegister) {
                    throw new InconsistentBranchConditionException();
                }
            }
            ignoreLoopException = false;
            mLastVM = mCurrentVM;
        } catch (ReachedReturnStatementException e) {
            if (mLastVM != null)
                return mLastVM; // the switch statement was reached in the first instance, continuing execution lead to a return
            throw e;
        }

        return mLastVM;
    }

    private void executeClinit() {
        this.mMethodManager.selectMethod("<clinit>", Lists.newArrayList(), "V");
        this.run(0, 15);
        this.mRegisters.clearAndInit();
    }

    public RegisterMap run(int startIndex, int stopAtIndex) {
        MainApp.log("SmaliVM", "Executing " + mMethodManager.getMethodName());
        this.currentInstructionIndex = startIndex;
        while (mMethodManager.hasNextInstruction(currentInstructionIndex) && currentInstructionIndex != stopAtIndex)
            step();
        MainApp.log("SmaliVM", "Finished executing at " + this.currentInstructionIndex + ", registers are " + this.mRegisters);
        return mRegisters;
    }


    private RegisterMap step() {
        BuilderInstruction nextInstruction = mMethodManager.getNextInstruction(currentInstructionIndex);
        processInstruction(nextInstruction);
        return this.mRegisters;
    }

    public void jump(Label target) {
        int targetIndex = target.getLocation().getIndex() - 1;
        if (jumpAddressList.contains(targetIndex) && !ignoreLoopException) {
            throw new LoopException(jumpAddressList.get(0));
        }
        jumpAddressList.add(targetIndex);
        this.currentInstructionIndex = targetIndex;
    }

    public void processInstruction(BuilderInstruction mInstruction) {
        int vX = 0;
        int vY = 1;
        int vZ = 2;

        int literal = 0;
        Label offset = null;
        String mFieldReference = null;
        String mMethodReference = null;

        if (mInstruction instanceof OneRegisterInstruction) {
            int mReg = ((OneRegisterInstruction) mInstruction).getRegisterA();
            vX = this.mRegisters.get(mReg).getPrimitiveValue();
        }
        if (mInstruction instanceof TwoRegisterInstruction) {
            int mReg = ((TwoRegisterInstruction) mInstruction).getRegisterB();
            if (this.mRegisters.get(mReg).getState() == RegisterState.UNINITIALIZED)
                throw new RegisterNotInitializedException(mReg);
            vY = this.mRegisters.get(mReg).getPrimitiveValue();
        }
        if (mInstruction instanceof ThreeRegisterInstruction) {
            int mReg = ((ThreeRegisterInstruction) mInstruction).getRegisterC();
            if (this.mRegisters.get(mReg).getState() == RegisterState.UNINITIALIZED)
                throw new RegisterNotInitializedException(mReg);
            vZ = this.mRegisters.get(mReg).getPrimitiveValue();
        }
        if (mInstruction instanceof NarrowLiteralInstruction) {
            literal = ((NarrowLiteralInstruction) mInstruction).getNarrowLiteral();
        }
        if (mInstruction instanceof ReferenceInstruction) {
            Reference mReference = ((ReferenceInstruction) mInstruction).getReference();
            if (mReference instanceof FieldReference) {
                mFieldReference = ((FieldReference) mReference).getName();
            } else if (mReference instanceof MethodReference) {
                mMethodReference = ((MethodReference) mReference).getName();
            }
        }
        if (mInstruction instanceof BuilderOffsetInstruction) {
            offset = ((BuilderOffsetInstruction) mInstruction).getTarget();
        }


        switch (mInstruction.getOpcode()) {
            case NOP:
                break;
            case CONST_WIDE:
            case CONST_HIGH16:
            case CONST_WIDE_32:
            case CONST_4:
            case CONST_16:
            case CONST_WIDE_16:
            case CONST:
                vX = literal;
                break;
            case SPUT_BOOLEAN:
            case SPUT_BYTE:
            case SPUT_CHAR:
            case SPUT_SHORT:
            case SPUT_WIDE:
            case SPUT:
                mFields.put(mFieldReference, vX);
                break;
            case SGET_BOOLEAN:
            case SGET_BYTE:
            case SGET_CHAR:
            case SGET_SHORT:
            case SGET:
                vX = mFields.get(mFieldReference).getPrimitiveValue();
                break;
            case XOR_INT:
                vX = vY ^ vZ;
                break;
            case XOR_INT_2ADDR:
                vX = vX ^ vY;
                break;
            case XOR_INT_LIT8:
            case XOR_INT_LIT16:
                vX = vY ^ literal;
                break;
            case NEG_INT:
                vX = -vY;
                break;
            case GOTO:
            case GOTO_16:
            case GOTO_32:
                jump(offset);
                break;
            case AND_INT:
                vX = vY & vZ;
                break;
            case AND_INT_2ADDR:
                vX = vX & vY;
                break;
            case AND_INT_LIT8:
            case AND_INT_LIT16:
                vX = vY & literal;
                break;
            case REM_INT:
                vX = vY % vZ;
                break;
            case REM_INT_2ADDR:
                vX = vX % vY;
                break;
            case REM_INT_LIT8:
            case REM_INT_LIT16:
                vX = vY % literal;
                break;
            case ADD_INT:
                vX = vY + vZ;
                break;
            case ADD_INT_2ADDR:
                vX = vX + vY;
                break;
            case ADD_INT_LIT8:
            case ADD_INT_LIT16:
                vX = vY + literal;
                break;
            case SUB_INT:
            case SUB_LONG:
            case SUB_DOUBLE:
                vX = vY - vZ;
                break;
            case SUB_INT_2ADDR:
            case SUB_LONG_2ADDR:
            case SUB_DOUBLE_2ADDR:
                vX = vX - vY;
                break;
            case OR_INT:
                vX = vY | vZ;
                break;
            case OR_INT_2ADDR:
                vX = vX | vY;
                break;
            case OR_INT_LIT8:
            case OR_INT_LIT16:
                vX = vY | literal;
                break;
            case IF_EQZ:
                if (vX == 0)
                    jump(offset);
                break;
            case IF_GEZ: // lit >= 0
                if (vX >= 0)
                    jump(offset);
                break;
            case IF_GTZ:
                if (vX > 0)
                    jump(offset);
                break;
            case IF_LEZ:
                if (vX <= 0)
                    jump(offset);
                break;
            case IF_LTZ:
                if (vX < 0)
                    jump(offset);
                break;
            case IF_NEZ:
                if (vX != 0)
                    jump(offset);
                break;
            case IF_EQ:
                if (vX == vY)
                    jump(offset);
                break;
            case IF_GE:
                if (vX >= vY)
                    jump(offset);
                break;
            case IF_GT:
                if (vX > vY)
                    jump(offset);
                break;
            case IF_LE:
                if (vX <= vY)
                    jump(offset);
                break;
            case IF_LT:
                if (vX < vY)
                    jump(offset);
                break;
            case IF_NE:
                if (vX != vY)
                    jump(offset);
                break;
            case SHL_INT_LIT8:
            case SHR_INT_LIT8:
                vX = vY << literal;
                break;
            case SHL_INT:
            case SHL_LONG:
            case SHR_LONG:
                vX = vY << vZ;
                break;
            case SHL_INT_2ADDR:
            case SHL_LONG_2ADDR:
                vX = vX << vY;
                break;
            case SHR_INT_2ADDR:
            case SHR_LONG_2ADDR:
                vX = vX >> vY;
                break;
            case SHR_INT:
                vX = vY >> vZ;
                break;
            case USHR_INT:
            case USHR_LONG:
                vX = vY >>> vZ;
                break;
            case USHR_INT_LIT8:
                vX = vY >>> literal;
                break;
            case USHR_INT_2ADDR:
            case USHR_LONG_2ADDR:
                vX = vX >>> vY;
                break;
            case MOVE_16:
            case MOVE:
            case MOVE_FROM16:
            case MOVE_WIDE:
            case MOVE_WIDE_16:
            case MOVE_WIDE_FROM16:
                vX = vY;
                break;
            case MOVE_RESULT_WIDE:
                vY = 0xCAFEBABE;
            case MOVE_RESULT:
                // TODO unknown register
                break;
            case MOVE_EXCEPTION:
            case MOVE_OBJECT:

            case MOVE_OBJECT_16:
            case MOVE_OBJECT_FROM16:
            case MOVE_RESULT_OBJECT:
                vX = 0xCAFEBABE;
                break;
            case PACKED_SWITCH:
                BuilderInstruction31t packedSwitch = (BuilderInstruction31t) mInstruction;
                BuilderPackedSwitchPayload mPayload = (BuilderPackedSwitchPayload) packedSwitch.getTarget().getLocation().getInstruction();
                assert mPayload != null;
                for (BuilderSwitchElement switchElement : mPayload.getSwitchElements()) {
                    if (vX == switchElement.getKey()) {
                        jump(switchElement.getTarget());
                        break;
                    }
                }
                break;
            case INVOKE_STATIC:
            case INVOKE_VIRTUAL:
            case INVOKE_INTERFACE:
            case INVOKE_DIRECT:
            case INVOKE_SUPER:
                break;
            case RETURN:
            case RETURN_VOID:
            case RETURN_WIDE:
            case RETURN_OBJECT:
            case RETURN_VOID_BARRIER:
            case RETURN_VOID_NO_BARRIER:
                throw new ReachedReturnStatementException();
            default:
                // System.out.println("Unknown Instruction: " + FormatHelper.instructionToString(mInstruction));
            break;
        }

        if (mInstruction instanceof ThreeRegisterInstruction) {
            int mReg = ((ThreeRegisterInstruction) mInstruction).getRegisterC();
            this.mRegisters.put(mReg, vZ);
        }
        if (mInstruction instanceof TwoRegisterInstruction) {
            int mReg = ((TwoRegisterInstruction) mInstruction).getRegisterB();
            this.mRegisters.put(mReg, vY);
        }
        if (mInstruction instanceof OneRegisterInstruction) {
            int mReg = ((OneRegisterInstruction) mInstruction).getRegisterA();
            this.mRegisters.put(mReg, vX);
        }

        // System.out.println("SmaliVM: Executed Instruction " + FormatHelper.instructionToString(mInstruction));

        currentInstructionIndex++;
    }
}
