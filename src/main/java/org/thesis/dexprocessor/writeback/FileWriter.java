package org.thesis.dexprocessor.writeback;

import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.DexIO;
import lanchon.multidexlib2.MultiDexIO;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.thesis.ListenerThread;
import org.thesis.MainApp;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileWriter extends ListenerThread {

    private List<ClassDef> mDeobfuscatedClassDefList;
    private File patchedFile;

    public FileWriter(List<ClassDef> mDeobfuscatedClassDefList, File patchedFile) {
        this.mDeobfuscatedClassDefList = mDeobfuscatedClassDefList;
        this.patchedFile = patchedFile;
    }

    private void writeToDex() {
        try {
            MultiDexIO.writeDexFile(false, patchedFile, new BasicDexFileNamer(), new DexFile() {
                @Nonnull
                @Override
                public Set<? extends ClassDef> getClasses() {
                    return new HashSet<>(mDeobfuscatedClassDefList);
                }

                @Nonnull
                @Override
                public Opcodes getOpcodes() {
                    return Opcodes.getDefault();
                }
            }, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null);
            MainApp.log("FileWriter", "Wrote dex file to " + patchedFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        writeToDex();
        notifyListeners();
    }
}
