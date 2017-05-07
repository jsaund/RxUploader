package com.jagsaund.rxuploader.job;

import android.support.annotation.NonNull;

public enum StatusType {
    QUEUED("queued"),
    SENDING("sending"),
    COMPLETED("completed"),
    FAILED("failed"),
    INVALID("invalid");

    @NonNull private final String type;

    StatusType(@NonNull String type) {
        this.type = type;
    }

    @NonNull
    static StatusType fromString(@NonNull String type) {
        switch (type) {
            case "queued": {
                return QUEUED;
            }
            case "sending": {
                return SENDING;
            }
            case "completed": {
                return COMPLETED;
            }
            case "failed": {
                return FAILED;
            }
            case "invalid":
            default: {
                return INVALID;
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return type;
    }
}