package org.thesis.dexprocessor.instructions;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;

public class NopInstruction extends BuilderInstruction10x {

    public NopInstruction() {
        super(Opcode.NOP);
    }

    @Override
    public Format getFormat() {
        return Opcode.NOP.format;
    }
}
