package com.jagsaund.rxuploader.job;

import android.support.annotation.NonNull;
import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

@GsonTypeAdapterFactory
public abstract class JobTypeAdapterFactory implements TypeAdapterFactory {
    @NonNull
    public static TypeAdapterFactory create() {
        return new AutoValueGson_JobTypeAdapterFactory();
    }
}
