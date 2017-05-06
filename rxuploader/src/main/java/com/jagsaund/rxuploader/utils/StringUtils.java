package com.jagsaund.rxuploader.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class StringUtils {
    private StringUtils() {
    }

    public static boolean isNullOrEmpty(@Nullable CharSequence s) {
        return s == null || s.length() == 0;
    }

    @NonNull
    public static String getOrDefault(@Nullable String s, @NonNull String t) {
        return s != null ? s : t;
    }

    @NonNull
    public static String getOrEmpty(@Nullable String s) {
        return getOrDefault(s, "");
    }
}
