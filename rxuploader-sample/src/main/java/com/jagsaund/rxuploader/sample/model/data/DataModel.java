package com.jagsaund.rxuploader.sample.model.data;

import android.support.annotation.NonNull;

public interface DataModel {
    @DataModelType
    int type();

    @NonNull
    String id();
}
