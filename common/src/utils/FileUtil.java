package utils;

import org.apache.commons.io.FileUtils;

import java.io.*;

public class FileUtil extends FileUtils {

    public static boolean fileExists(String filePath) throws IOException {
        return (new File(filePath).getCanonicalFile().exists());
    }

    public static boolean isDir(String dirPath) throws IOException {
        return (
                (new File(dirPath).getCanonicalFile().exists())
                        &&
                        (new File(dirPath).getCanonicalFile().isDirectory()));
    }

    public static void putFile(byte[] data, String target, boolean append)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(target, append);
        fos.write(data);
        fos.flush();
        fos.close();
    }

}
