package com.bloxbean.cardano.yaci.store.common.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtil {

    public static String readFile(String url) {
        StringBuilder content = new StringBuilder();
        try (BufferedInputStream fileGenesis = new BufferedInputStream(new FileInputStream(url))) {
            byte[] bytes = new byte[500];
            while (fileGenesis.available() != 0) {
                fileGenesis.read(bytes);
                content.append(new String(bytes));
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return content.toString();
    }

    public static String readFileFromClasspath(String path) {
        StringBuilder content = new StringBuilder();
        try (BufferedInputStream fileGenesis = new BufferedInputStream(FileUtil.class.getClassLoader().getResourceAsStream(path))) {
            byte[] bytes = new byte[500];
            while (fileGenesis.available() != 0) {
                fileGenesis.read(bytes);
                content.append(new String(bytes));
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return content.toString();
    }
}

