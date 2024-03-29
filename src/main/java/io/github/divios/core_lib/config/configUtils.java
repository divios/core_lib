package io.github.divios.core_lib.config;

import java.io.*;

public class configUtils {

    /**
     * Copy all the contents from one file to another
     */
    public static void copyContents(File from, File to) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(from);
        } catch (Exception e) {
            e.printStackTrace();
        }
        copyContents(in, to);
    }

    public static void copyContents(InputStream in, File to) throws IOException {

        OutputStream out = new FileOutputStream(to);
        byte[] buffer = new byte[1024];
        int lenght = in.read(buffer);
        while (lenght != -1) {
            out.write(buffer, 0, lenght);
            lenght = in.read(buffer);
        }

    }

    public static void createFile(File f) {
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
