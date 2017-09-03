package io.whz.androidneuralnetwork.utils;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class DecompressUtils {
    private static final int LABEL_MAGIC = 2049;
    private static final int IMAGE_MAGIC = 2051;
    private static final int BATCH_MAGIC = 2052;
    private static final int FILE_MINI_BATCH = 2000;
    private static final String BATCH_FILE_SUFFIX = "batch";

    public static boolean gunzip(File sourceFile, File targetFile) {
        if (!sourceFile.exists()) {
            return false;
        }

        GZIPInputStream inputStream = null;
        FileOutputStream outputStream = null;

        boolean result;

        try {
            inputStream = new GZIPInputStream(new FileInputStream(sourceFile));
            outputStream = new FileOutputStream(targetFile);

            int len;
            final byte[] buffer = new byte[1024];

            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            outputStream.flush();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();

            result = false;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return result;
    }

    public static List<Integer> parseLabels(File sourceFile) {
        DataInputStream inputStream = null;
        final List<Integer> list = new ArrayList<>();

        try {
            inputStream = new DataInputStream(new FileInputStream(sourceFile));

            final int magic = inputStream.readInt();

            if (magic != LABEL_MAGIC) {
                throw new IllegalArgumentException();
            }

            final int len = inputStream.readInt();
            byte cur;

            for (int i = 0; i < len; ++i) {
                cur = inputStream.readByte();
                list.add((int) cur);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return list;
    }

    public static byte[] test(File file) {

        byte b = -1;
        Log.i("test2", (b & 0xff) + "");

        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(file));

            final int magic = inputStream.readInt();
            final int len = inputStream.readInt();
            final int rows = inputStream.readInt();
            final int cols = inputStream.readInt();
            final int label = inputStream.readInt();

            byte[] buffer = new byte[28 * 28];
            inputStream.read(buffer);
            return buffer;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean parseImages(File sourceFile, File targetDir, String prefix, List<Integer> labels) {
        if (!sourceFile.exists() || !targetDir.exists()) {
            return false;
        }

        DataInputStream inputStream = null;
        DataOutputStream outputStream = null;

        boolean result;

        try {
            inputStream = new DataInputStream(new FileInputStream(sourceFile));

            final int magic = inputStream.readInt();

            if (magic != IMAGE_MAGIC) {
                throw new IllegalArgumentException();
            }

            final int len = inputStream.readInt();

            if (len != labels.size()) {
                throw new IllegalStateException();
            }

            final int rows = inputStream.readInt();
            final int cols = inputStream.readInt();
            final byte[] buffer = new byte[rows * cols];
            int count = 1;

            for (int i = 1; i <= len; ++i) {
                if (outputStream == null) {
                    outputStream = new DataOutputStream(new FileOutputStream(new File(targetDir,
                            String.format("%s_%s.%s", prefix, count++, BATCH_FILE_SUFFIX))));
                    outputStream.writeInt(BATCH_MAGIC);
                    outputStream.writeInt(FILE_MINI_BATCH);
                    outputStream.writeInt(rows);
                    outputStream.writeInt(cols);
                }

                inputStream.read(buffer);
                outputStream.writeInt(labels.get(i - 1));
                outputStream.write(buffer);

                if (i % FILE_MINI_BATCH == 0) {
                    outputStream.flush();
                    outputStream.close();
                    outputStream = null;
                }
            }

            result = true;
        } catch (IOException e) {
            e.printStackTrace();

            result = false;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
}
