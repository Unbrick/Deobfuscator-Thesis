package org.thesis.ui.fxelements;

import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.instruction.*;
import org.thesis.dexprocessor.FormatHelper;

public class FXListInstruction {

    private Instruction mInstruction;
    private TryBlock mTryBlock;

    public FXListInstruction(TryBlock mTryBlock) {
        this.mTryBlock = mTryBlock;
    }

    public FXListInstruction(Instruction mInstruction) {
        this.mInstruction = mInstruction;
    }

    public Instruction getInstruction() {
        return mInstruction;
    }

    @Override
    public String toString() {
        if (mTryBlock == null) {
            return FormatHelper.instructionToString(mInstruction);
        } else {
            //TODO add try block parsing
            return mTryBlock.toString();
        }
    }
}
