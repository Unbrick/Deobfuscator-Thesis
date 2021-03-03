package org.thesis.dexprocessor.vm;

import com.google.common.collect.Lists;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.BuilderOffsetInstruction;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31t;
import org.jf.dexlib2.builder.instruction.BuilderPackedSwitchPayload;
import org.jf.dexlib2.builder.instruction.BuilderSwitchElement;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.thesis.Logger;
import org.thesis.dexprocessor.exceptions.InconsistentBranchConditionException;
import org.thesis.dexprocessor.exceptions.LoopException;
import org.thesis.dexprocessor.exceptions.ReachedReturnStatementException;
import org.thesis.dexprocessor.exceptions.RegisterNotInitializedException;
import org.thesis.dexprocessor.vm.instancefields.FieldMap;
import org.thesis.dexprocessor.vm.registers.RegisterMap;

import java.util.ArrayList;
import java.util.List;

import static org.thesis.dexprocessor.vm.registers.RegisterState.*;

public class SmaliVM {

    private static SmaliVM instance;

    // insecure mode is set in case the execution starting from the begin did not work
    // it only allows matching register values with branch conditions, in case the default branch would be taken, optimisation is skipped
    private boolean insecureMode = false;
    public int currentInstructionIndex = 0;
    public boolean ignoreLoopException = false;
    public boolean ignoreReturnStatementException = false;
    public List<Integer> jumpAddressList = new ArrayList<>();

    public RegisterMap mRegisters;
    public FieldMap mFields;
    public MethodManager mMethodManager;

    public static SmaliVM getInstance(ClassDef mClass, Method mMethod, MutableMethodImplementation mMutable) {
        if (instance == null)
            instance = new SmaliVM(mClass, mMethod, mMutable);

        if (!mMethod.getName().equals("<clinit>")) {
            instance.currentInstructionIndex = 0;
            instance.jumpAddressList = Lists.newArrayList();
            instance.mFields.clearAndInit();
            instance.executeClinit();
        }

        instance.insecureMode = false;
        instance.mMethodManager.selectMethod(mMethod, mMutable);
        instance.currentInstructionIndex = 0;
        instance.jumpAddressList = Lists.newArrayList();
        instance.mRegisters.clearAndInit(mMethod);

        return instance;
    }

    private SmaliVM(ClassDef mClass, Method mMethod, MutableMethodImplementation mMutable) {
        this.currentInstructionIndex = 0;
        this.mRegisters = new RegisterMap(50);
        this.mFields = new FieldMap(mClass.getFields());
        this.mMethodManager = new MethodManager(mClass, mMethod, mMutable);
    }

    public boolean isInsecureMode() {
        return insecureMode;
    }

    public RegisterMap runWithInconsistentBranchConditionDetection(int stopAtIndex) {
        RegisterMap mVMRegisters = null;

        for (int i = 1; i <= 3; i++) {
            // Try starting from 0, if this does not work, try to start from packed-switch - 5; packed-switch - 10; packed-switch - 15
            // This can circumvent unknown branches or register values
            int startIndex = i == 1 ? 0 : Math.max((stopAtIndex - i * 10), 0);
            try {
                if (i > 1) {
                    insecureMode = true;
                }
                mVMRegisters = this.run(startIndex, stopAtIndex);
                // if the run succeded, break the loop
                break;
            } catch (InconsistentBranchConditionException | RegisterNotInitializedException e) {
                Logger.debug("SmaliVM", "Execution failed from index "+ startIndex +", trying from index " + Math.max((stopAtIndex - i * 10), 0));
            }
        }
        try {
            // continue to run from the current instruction index to see if we are running in a loop
            // This instance of run should throw a ReachedReturnStatementException or any other exception
            // If it terminates at the packed-switch instruction - 1 we are most probably running in a loop
            this.run(currentInstructionIndex, currentInstructionIndex - 1);
            // If we terminate here the packed-switch is running inside a loop and we cannot optimize it
            throw new LoopException(0xDEADBEEF);
        } catch (ReachedReturnStatementException | RegisterNotInitializedException e) {
            if (mVMRegisters != null)
                return mVMRegisters; // the switch statement was reached in the first instance, continuing execution lead to a return
            throw e;
        }
    }

    private void executeClinit() {
        this.mMethodManager.selectMethod("<clinit>", Lists.newArrayList(), "V");
        this.run(0, 10);
        this.mRegisters.clearAndInit(this.mMethodManager.getMethod());
    }

    public RegisterMap run(int startIndex, int stopAtIndex) {
        Logger.debug("SmaliVM", "Executing " + mMethodManager.getMethodName());
        this.currentInstructionIndex = startIndex;
        while (mMethodManager.hasNextInstruction(currentInstructionIndex) && currentInstructionIndex != stopAtIndex)
            step();
        Logger.debug("SmaliVM", "Finished executing at " + this.currentInstructionIndex + ", registers are " + this.mRegisters);
        return new RegisterMap(mRegisters);
    }


    private RegisterMap step() {
        BuilderInstruction nextInstruction = mMethodManager.getNextInstruction(currentInstructionIndex);
        processInstruction(nextInstruction);
        return this.mRegisters;
    }

    public void jump(Label target) {
        // decrease the target index to counter the increment at the end of opcode execution
        int targetIndex = target.getLocation().getIndex() - 1;
        // when hitting the same address for the third time throw a loop exception
        if (jumpAddressList.stream().filter(integer -> integer.equals(targetIndex)).count() > 15) {
            throw new LoopException(jumpAddressList.get(0));
        }
        jumpAddressList.add(targetIndex);
        this.currentInstructionIndex = targetIndex;
    }

    public void processInstruction(BuilderInstruction mInstruction) {
        int vX = -1;
        int vY = -1;
        int vZ = -1;

        int numvX = -1;
        int numvY = -1;
        int numvZ = -1;

        int literal = 0;
        Label offset = null;
        String mFieldReference = null;
        String mMethodReference = null;

        if (mInstruction instanceof OneRegisterInstruction) {
            numvX = ((OneRegisterInstruction) mInstruction).getRegisterA();
            vX = this.mRegisters.get(numvX).getPrimitiveValue();
        }
        if (mInstruction instanceof TwoRegisterInstruction) {
            numvY = ((TwoRegisterInstruction) mInstruction).getRegisterB();
            vY = this.mRegisters.get(numvY).getPrimitiveValue();
        }
        if (mInstruction instanceof ThreeRegisterInstruction) {
            numvZ = ((ThreeRegisterInstruction) mInstruction).getRegisterC();
            vZ = this.mRegisters.get(numvZ).getPrimitiveValue();
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
            case IGET_OBJECT:
            case IGET_BOOLEAN:
            case IGET_BOOLEAN_QUICK:
            case IGET_BYTE:
            case IGET_BYTE_QUICK:
            case IGET:
            case IGET_CHAR:
            case IGET_CHAR_QUICK:
            case IGET_OBJECT_QUICK:
            case IGET_OBJECT_VOLATILE:
            case IGET_QUICK:
            case IGET_SHORT:
            case IGET_SHORT_QUICK:
            case IGET_VOLATILE:
            case IGET_WIDE:
            case IGET_WIDE_QUICK:
            case IGET_WIDE_VOLATILE:
                vX = assign_field(mFieldReference);
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
                check_register_state_known(numvX);
                //TODO if register state is unknown, take both poss
                if (vX == 0)
                    jump(offset);
                break;
            case IF_GEZ:
                check_register_state_known(numvX);
                if (vX >= 0)
                    jump(offset);
                break;
            case IF_GTZ:
                check_register_state_known(numvX);
                if (vX > 0)
                    jump(offset);
                break;
            case IF_LEZ:
                check_register_state_known(numvX);
                if (vX <= 0)
                    jump(offset);
                break;
            case IF_LTZ:
                check_register_state_known(numvX);
                if (vX < 0)
                    jump(offset);
                break;
            case IF_NEZ:
                check_register_state_known(numvX);
                if (vX != 0)
                    jump(offset);
                break;
            case IF_EQ:
                check_register_state_known(numvX, numvY);
                if (vX == vY)
                    jump(offset);
                break;
            case IF_GE:
                check_register_state_known(numvX, numvY);
                if (vX >= vY)
                    jump(offset);
                break;
            case IF_GT:
                check_register_state_known(numvX, numvY);
                if (vX > vY)
                    jump(offset);
                break;
            case IF_LE:
                check_register_state_known(numvX, numvY);
                if (vX <= vY)
                    jump(offset);
                break;
            case IF_LT:
                check_register_state_known(numvX, numvY);
                if (vX < vY)
                    jump(offset);
                break;
            case IF_NE:
                check_register_state_known(numvX, numvY);
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
                vY = 0xF0F0F0;
            case MOVE_RESULT:
                vX = 0xF0F0F0;
                break;
            case MOVE_EXCEPTION:
            case MOVE_OBJECT:
            case MOVE_OBJECT_16:
            case MOVE_OBJECT_FROM16:
            case MOVE_RESULT_OBJECT:
                vX = 0xF0F0F0;
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

        currentInstructionIndex++;

        if (mInstruction instanceof OneRegisterInstruction) {
            int _vX = ((OneRegisterInstruction) mInstruction).getRegisterA();
            if (mInstruction instanceof TwoRegisterInstruction) {
                int _vY = ((TwoRegisterInstruction) mInstruction).getRegisterB();
                if (mRegisters.get(_vY).getState() == UNINITIALIZED || mRegisters.get(_vY).getState() == UNKNOWN) {
                    //register value vX depends on unknown or uninitialized register value vY, populate vX as unknown
                    //no writeback is required
                    mRegisters.get(_vX).setState(UNKNOWN);
                    return;
                }
                if (mInstruction instanceof ThreeRegisterInstruction) {
                    int _vZ = ((ThreeRegisterInstruction) mInstruction).getRegisterC();
                    if (mRegisters.get(_vZ).getState() == UNINITIALIZED || mRegisters.get(_vZ).getState() == UNKNOWN) {
                        //register value vX depends on unknown or uninitialized register value vZ, populate vX as unknown
                        //no writeback is required
                        mRegisters.get(_vX).setState(UNKNOWN);
                        return;
                    }
                }
            }

            // lets check whether a object is hiding in the vX register
            if (vX == 0xF0F0F0) {
                mRegisters.get(_vX).setState(OBJECT);
                return;
            } else if (vX == 0xA0A0A0) {
                mRegisters.get(_vX).setState(UNKNOWN);
                return;
            }

            // reaching this point means either the instruction is a one register instructions
            // or otherwise all other registers involved are known
            // we can write back the register value as new integer value
            this.mRegisters.put(_vX, vX);
        }
    }

    private int assign_field(String mFieldReference) {
        if (mFields.containsKey(mFieldReference) && mFields.get(mFieldReference).getState().isPrimitive())
            return mFields.get(mFieldReference).getPrimitiveValue();
        else if (mFields.containsKey(mFieldReference) && mFields.get(mFieldReference).getState().isNonPrimitive())
            return 0xF0F0F0;
        else return 0xA0A0A0;
    }

    private void check_register_state_known(int vX, int vY) {
        check_register_state_known(vX);
        check_register_state_known(vY);
    }

    private void check_register_state_known(int vX) {
        if (mRegisters.get(vX).getState() == UNKNOWN || mRegisters.get(vX).getState() == UNINITIALIZED || mRegisters.get(vX).getState() == OBJECT)
            throw new RegisterNotInitializedException(vX);
    }
}
