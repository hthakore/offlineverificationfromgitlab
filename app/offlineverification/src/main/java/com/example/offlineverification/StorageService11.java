package com.example.offlineverification;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.vosk.SpeakerModel;
import org.vosk.android.StorageService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StorageService11 {

    protected static final String TAG = StorageService.class.getSimpleName();

    public StorageService11() {
    }

    public static void unpack11(Context context, String sourcePath, String targetPath, Callback<SpeakerModel> completeCallback, Callback<IOException> errorCallback) {
        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                String outputPath = sync11(context, sourcePath, targetPath);
                SpeakerModel model = new SpeakerModel(outputPath);
                handler.post(() -> {
                    completeCallback.onComplete(model);
                });
            } catch (IOException var8) {
                handler.post(() -> {
                    errorCallback.onComplete(var8);
                });
            }

        });
    }

    public static String sync11(Context context, String sourcePath, String targetPath) throws IOException {
        AssetManager assetManager = context.getAssets();
        File externalFilesDir = context.getExternalFilesDir((String)null);
        if (externalFilesDir == null) {
            throw new IOException("cannot get external files dir, external storage state is " + Environment.getExternalStorageState());
        } else {
            File targetDir = new File(externalFilesDir, targetPath);
            String resultPath = (new File(targetDir, sourcePath)).getAbsolutePath();
            String sourceUUID = readLine11(assetManager.open(sourcePath + "/uuid"));

            try {
                String targetUUID = readLine11(new FileInputStream(new File(targetDir, sourcePath + "/uuid")));
                if (targetUUID.equals(sourceUUID)) {
                    return resultPath;
                }
            } catch (FileNotFoundException var9) {
            }

            deleteContents11(targetDir);
            copyAssets11(assetManager, sourcePath, targetDir);
            copyFile11(assetManager, sourcePath + "/uuid", targetDir);
            return resultPath;
        }
    }

    private static String readLine11(InputStream is) throws IOException {
        return (new BufferedReader(new InputStreamReader(is))).readLine();
    }

    private static boolean deleteContents11(File dir) {
        File[] files = dir.listFiles();
        boolean success = true;
        if (files != null) {
            File[] var3 = files;
            int var4 = files.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                File file = var3[var5];
                if (file.isDirectory()) {
                    success &= deleteContents11(file);
                }

                if (!file.delete()) {
                    success = false;
                }
            }
        }

        return success;
    }

    private static void copyAssets11(AssetManager assetManager, String path, File outPath) throws IOException {
        String[] assets = assetManager.list(path);
        if (assets != null) {
            if (assets.length == 0) {
                if (!path.endsWith("uuid")) {
                    copyFile11(assetManager, path, outPath);
                }
            } else {
                File dir = new File(outPath, path);
                if (!dir.exists()) {
                    Log.v(TAG, "Making directory " + dir.getAbsolutePath());
                    if (!dir.mkdirs()) {
                        Log.v(TAG, "Failed to create directory " + dir.getAbsolutePath());
                    }
                }

                String[] var5 = assets;
                int var6 = assets.length;

                for(int var7 = 0; var7 < var6; ++var7) {
                    String asset = var5[var7];
                    copyAssets11(assetManager, path + "/" + asset, outPath);
                }
            }

        }
    }

    private static void copyFile11(AssetManager assetManager, String fileName, File outPath) throws IOException {
        Log.v(TAG, "Copy " + fileName + " to " + outPath);
        InputStream in = assetManager.open(fileName);
        OutputStream out = new FileOutputStream(outPath + "/" + fileName);
        byte[] buffer = new byte[4000];

        int read;
        while((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

        in.close();
        out.close();
    }

    public interface Callback<R> {
        void onComplete(R var1);
    }

}
