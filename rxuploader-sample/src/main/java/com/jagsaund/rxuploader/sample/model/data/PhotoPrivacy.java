package com.jagsaund.rxuploader.sample.model.data;

public enum PhotoPrivacy {
    PUBLIC(0),
    PRIVATE(1);

    public final int value;
    PhotoPrivacy(int value) {
        this.value = value;
    }
}
