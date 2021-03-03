package org.thesis.dexprocessor.writeback;

import org.jf.dexlib2.Opcode;

public class Rewriter{
    public static boolean opcodeIsBetween(Opcode start, Opcode end, Opcode opcode) {
        return start.ordinal() <= opcode.ordinal() && opcode.ordinal() <= end.ordinal();
    }
}
