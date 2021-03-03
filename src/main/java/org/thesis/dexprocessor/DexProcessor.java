package org.thesis.dexprocessor;

import javafx.scene.control.TreeItem;
import org.jf.dexlib2.iface.ClassDef;
import org.thesis.dexprocessor.Deobfuscator;
import org.thesis.dexprocessor.DexFileManager;
import org.thesis.dexprocessor.DexProcessorThread;
import org.thesis.dexprocessor.statistics.SanitizerStatistics;

import java.io.File;
import java.util.List;

public class DexProcessor {

    private ClassDef mSelectedClass;
    private String selectedPackage;
    private Deobfuscator mDeobfuscator;
    private DeobfuscatorFinishedListener mFinishedListener;
    private DexFileManager mDexFileManager;

    public void addListener(DeobfuscatorFinishedListener deobfuscatorFinishedListener) {
        this.mFinishedListener = deobfuscatorFinishedListener;
    }

    public void deobfuscatePackage(boolean mathEnabled, boolean branchEnabled, boolean switchCaseEnabled, boolean tryEnabled, boolean removeNops, boolean mathLogic, boolean swichCaseUnsafe, boolean switchCaseLunatic) {
        mDeobfuscator = new Deobfuscator(selectedPackage, mathEnabled, branchEnabled, switchCaseEnabled, tryEnabled, removeNops, mathLogic, swichCaseUnsafe, switchCaseLunatic);
        mDeobfuscator.addListener(thread -> mFinishedListener.finished());
        mDeobfuscator.start();
    }

    public void deobfuscateClass(boolean mathEnabled, boolean branchEnabled, boolean switchCaseEnabled, boolean tryEnabled, boolean removeNops, boolean mathLogic, boolean swichCaseUnsafe, boolean switchCaseLunatic) {
        mDeobfuscator = new Deobfuscator(mSelectedClass, mathEnabled, branchEnabled, switchCaseEnabled, tryEnabled, removeNops, mathLogic, swichCaseUnsafe, switchCaseLunatic);
        mDeobfuscator.addListener(thread -> mFinishedListener.finished());
        mDeobfuscator.start();
    }

    public ClassDef getClassFromName(String className) {
        mSelectedClass = mDexFileManager.getClassList().stream().filter(x -> x.getType().equals(className)).findFirst().orElse(null);
        return mSelectedClass;
    }

    public String getPackageFromName(TreeItem<String> name) {
        StringBuilder packageName = new StringBuilder(name.getValue());

        TreeItem<String> currentItem = name;
        while (currentItem.getParent() != null) {
            currentItem = currentItem.getParent();
            packageName.insert(0, currentItem.getValue() + "/");
        }
        // delete the "src." root leaf
        packageName.delete(0, 4);

        selectedPackage = packageName.toString();
        return selectedPackage;
    }

    public void loadDexFile(File file, DexProcessorThread.DexLoadingListener mLoadedCallback) {
        mDexFileManager = DexFileManager.load(file);

        DexProcessorThread mDexProcessor = new DexProcessorThread(mLoadedCallback, file);
        mDexProcessor.start();
    }

    public boolean isFinished() {
        return !mDeobfuscator.isAlive();
    }

    public void saveFile(File selectedDirectory, SaveFileFinishedListener fileSaveFinishedListener) {
        mDexFileManager.writeFile(selectedDirectory, thread -> fileSaveFinishedListener.saved(selectedDirectory.getAbsolutePath()));
    }

    public List<SanitizerStatistics> getStatistics() {
        return mDeobfuscator.getStatistics();
    }

    public interface DeobfuscatorFinishedListener {
        void finished();
    }

    public interface SaveFileFinishedListener {
        void saved(String filename);
    }
}
