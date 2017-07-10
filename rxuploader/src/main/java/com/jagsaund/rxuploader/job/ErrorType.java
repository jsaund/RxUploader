package com.jagsaund.rxuploader.job;

import android.support.annotation.NonNull;

public enum ErrorType {
    NETWORK("network"),
    FILE_NOT_FOUND("file_not_found"),
    SERVICE("service"),
    TERMINATED("terminated"),
    UNKNOWN("unknown");

    @NonNull private final String type;

    ErrorType(@NonNull String type) {
        this.type = type;
    }

    @NonNull
    @Override
    public String toString() {
        return "ErrorType: " + type;
    }
}