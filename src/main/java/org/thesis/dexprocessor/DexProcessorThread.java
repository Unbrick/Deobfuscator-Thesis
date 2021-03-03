package org.thesis.dexprocessor;

import com.google.common.collect.Lists;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.MultiDexIO;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DexProcessorThread extends Thread {

    private TreeItem<String> mRootTreeItem;
    private DexLoadingListener mLoadedCallback;

    private File mFilePath;

    public interface DexLoadingListener {
        void update(int current, int total);
        void loaded(TreeItem<String> mRootTreeItem);
    }

    public DexProcessorThread(DexLoadingListener mLoadedCallback, File mFilePath) {
        this.mRootTreeItem = new TreeItem<>("src");
        this.mLoadedCallback = mLoadedCallback;
        this.mFilePath = mFilePath;
    }

    @Override
    public void run() {
        processFile(mFilePath);
        mLoadedCallback.loaded(mRootTreeItem);
    }

    private void processFile(File file) {
        try {
            mRootTreeItem.getChildren().clear();

            DexFile mDexFile = MultiDexIO.readDexFile(true, file, new BasicDexFileNamer(), null, null);
            Set<? extends ClassDef> mClasses = mDexFile.getClasses();
            for (int i = 0; i < mClasses.size(); i++) {
                ClassDef[] mClassesArray = new ClassDef[mClasses.size()];
                mClasses.toArray(mClassesArray);

                addIfNotExists(mClassesArray[i]);
                updateAlertDialog(i, mClasses.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateAlertDialog(int current, int total) {
        mLoadedCallback.update(current, total);
    }

    public void addIfNotExists(ClassDef _class) {
        String[] classes = _class.getType().replaceFirst("L","").replace(";", "").split("/");

        TreeItem<String> mTreeItem = mRootTreeItem;
        var ref = new Object() {
            int i = 0;
        };
        while(ref.i < classes.length && mTreeItem.getChildren().stream().anyMatch(stringTreeItem -> stringTreeItem.getValue().equals(classes[ref.i]))) {
            mTreeItem = mTreeItem.getChildren().stream().filter(x -> x.getValue().equals(classes[ref.i])).findFirst().get();
            ref.i++;
        }
        while (ref.i < classes.length) {
            TreeItem<String> mLeafItem;
            if (ref.i == classes.length - 1) {
                ImageView mImageView;
                if ((_class.getAccessFlags() & AccessFlags.INTERFACE.getValue()) != 0) {
                    mImageView = new ImageView(String.valueOf(getClass().getResource("icon_interface.png")));
                } else if ((_class.getAccessFlags() & AccessFlags.ENUM.getValue()) != 0) {
                    mImageView = new ImageView(String.valueOf(getClass().getResource("icon_enum.png")));
                } else {
                    mImageView = new ImageView(String.valueOf(getClass().getResource("icon_class.png")));
                }

                mLeafItem = new TreeItem<>(classes[ref.i], mImageView);

                /*for (Method _method : _class.getMethods()) {
                    ImageView mMethodImageView = new ImageView(String.valueOf(getClass().getResource("icon_method.png")));
                    String mMethodParameters = _method.getParameters().size() == 0 ? "" : String.valueOf(_method.getParameters());
                    TreeItem<String> methodTreeItem = new TreeItem<>(_method.getName() + "(" + mMethodParameters + ")" + _method.getReturnType(), mMethodImageView);
                    mLeafItem.getChildren().add(methodTreeItem);
                }*/
            } else {
                ImageView mImageView = new ImageView(String.valueOf(getClass().getResource("icon_folder.png")));
                mLeafItem = new TreeItem<>(classes[ref.i], mImageView);
            }
            mTreeItem.getChildren().add(mLeafItem);
            mTreeItem = mLeafItem;
            ref.i++;
        }
    }
}
