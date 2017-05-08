package com.jagsaund.rxuploader.sample.model.data;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.job.Status;

public class UploadDataModel implements DataModel {
    @NonNull
    public static DataModel create(@NonNull String name, @NonNull String description,
            @NonNull Status status) {
        return new UploadDataModel(name, description, status);
    }

    @NonNull private final String name;
    @NonNull private final String description;
    @NonNull private final Status status;

    private UploadDataModel(@NonNull String name, @NonNull String description,
            @NonNull Status status) {
        this.name = name;
        this.description = description;
        this.status = status;
    }

    @Override
    public int type() {
        return DataModelType.UPLOAD;
    }

    @NonNull
    @Override
    public String id() {
        return status.id();
    }

    @NonNull
    @Override
    public Status getStatus() {
        return status;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getDescription() {
        return description;
    }
}
