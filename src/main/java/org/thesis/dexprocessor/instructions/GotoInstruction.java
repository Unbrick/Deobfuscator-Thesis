package org.thesis.dexprocessor.instructions;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.instruction.BuilderInstruction30t;

import javax.annotation.Nonnull;

public class GotoInstruction extends BuilderInstruction30t {

    public GotoInstruction(Label target) {
        this(Opcode.GOTO_32, target);
    }

    private GotoInstruction(@Nonnull Opcode opcode, @Nonnull Label target) {
        super(opcode, target);
    }

    @Override
    public Format getFormat() {
        return Opcode.GOTO_32.format;
    }
}
