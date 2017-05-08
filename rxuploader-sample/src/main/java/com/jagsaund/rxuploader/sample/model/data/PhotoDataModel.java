package com.jagsaund.rxuploader.sample.model.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.jagsaund.rxuploader.job.Status;
import com.jagsaund.rxuploader.sample.model.wire.PhotoJSONModel;
import com.jagsaund.rxuploader.utils.StringUtils;

public class PhotoDataModel implements DataModel {
    @NonNull
    public static DataModel create(@NonNull PhotoJSONModel model) {
        return new PhotoDataModel(String.valueOf(model.id()), model.name(),
                model.description(), model.imageUrl());
    }

    @NonNull private final String id;
    @NonNull private final String name;
    @NonNull private final String description;
    @NonNull private final String url;
    @NonNull private final Status status;

    private PhotoDataModel(@NonNull String id, @NonNull String name, @Nullable String description,
            @NonNull String url) {
        this.id = id;
        this.name = name;
        this.description = StringUtils.getOrEmpty(description);
        this.url = url;
        this.status = Status.createCompleted(id, null);
    }

    @DataModelType
    @Override
    public int type() {
        return DataModelType.PHOTO;
    }

    @NonNull
    @Override
    public String id() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    @Override
    public Status getStatus() {
        return status;
    }

    @NonNull
    public String getUrl() {
        return url;
    }
}
