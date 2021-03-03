package org.thesis.dexprocessor;

import com.google.common.collect.Lists;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.DexIO;
import lanchon.multidexlib2.MultiDexIO;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.thesis.ListenerThread;
import org.thesis.Logger;
import org.thesis.dexprocessor.writeback.FileWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DexFileManager {

    private DexFile mDexFile;
    private ArrayList<ClassDef> mClassDefList;
    private boolean isMultiDex;
    private static DexFileManager mInstance;

    private DexFileManager() {}

    public static DexFileManager load(@Nullable File mFile) {
        if (DexFileManager.mInstance == null) {
            DexFileManager.mInstance = new DexFileManager();
            mInstance._load(mFile);
        }
        return DexFileManager.mInstance;
    }

    public void _load(File mFile) {
        try {
            this.isMultiDex = MultiDexIO.readMultiDexContainer(mFile, new BasicDexFileNamer(), Opcodes.getDefault()).getDexEntryNames().size() > 1;
            this.mDexFile = MultiDexIO.readDexFile(this.isMultiDex, mFile, new BasicDexFileNamer(), Opcodes.getDefault(), null);
            this.mClassDefList = Lists.newArrayList(this.mDexFile.getClasses());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setClass(ClassDef mClassDef) {
        mClassDefList.stream().filter(classDef -> classDef.getType().equals(mClassDef.getType())).findFirst().ifPresent(classDef -> mClassDefList.set(mClassDefList.indexOf(classDef), mClassDef));
    }

    public ArrayList<ClassDef> getClassList() {
        return mClassDefList;
    }

    public DexFile getFile() {
        return this.mDexFile;
    }

    public Thread saveAndReload(ListenerThread.ThreadCompleteListener mListener) {
        FileWriter mWriter;
        try {
            Path tempDirWithPrefix = Files.createTempDirectory("dextemp-");
            mWriter = new FileWriter(this.mClassDefList, tempDirWithPrefix.toFile(), this.isMultiDex);
            mWriter.addListener(thread -> _load(tempDirWithPrefix.toFile()));
            mWriter.addListener(mListener);
            mWriter.start();
            return mWriter;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void writeFile(File mFile, ListenerThread.ThreadCompleteListener mListener) {
        FileWriter mWriter = new FileWriter(this.mClassDefList, mFile, this.isMultiDex);
        mWriter.addListener(mListener);
        mWriter.start();
    }

    private static class FileWriter extends ListenerThread {

        private final List<ClassDef> mDeobfuscatedClassDefList;
        private final File patchedFile;
        private final boolean multiDex;

        public FileWriter(List<ClassDef> mDeobfuscatedClassDefList, File patchedFile, boolean multiDex) {
            this.mDeobfuscatedClassDefList = mDeobfuscatedClassDefList;
            this.patchedFile = patchedFile;
            this.multiDex = multiDex;
        }


        @Override
        public void run() {
            try {
                MultiDexIO.writeDexFile(multiDex, patchedFile, new BasicDexFileNamer(), new DexFile() {
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
                Logger.info("FileWriter", "Wrote dex file to " + patchedFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            notifyListeners();
        }
    }
}
