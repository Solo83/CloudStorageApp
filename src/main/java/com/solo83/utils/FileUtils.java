package com.solo83.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

@UtilityClass
public class FileUtils {

    @NotNull
    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB", "PB", "EB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static long convertToBytes(String size) {
        size = size.toUpperCase();
        if (size.endsWith("MB")) {
            return Long.parseLong(size.replace("MB", "").trim()) * 1024 * 1024;
        } else if (size.endsWith("KB")) {
            return Long.parseLong(size.replace("KB", "").trim()) * 1024;
        } else if (size.endsWith("GB")) {
            return Long.parseLong(size.replace("GB", "").trim()) * 1024 * 1024 * 1024;
        } else {
            return Long.parseLong(size.replace("B", "").trim());
        }
    }
}
