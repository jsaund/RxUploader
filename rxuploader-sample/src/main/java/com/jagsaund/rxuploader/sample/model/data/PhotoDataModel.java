package com.jagsaund.rxuploader.sample.model.data;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.sample.model.wire.PhotoJSONModel;

public class PhotoDataModel implements DataModel {
    @NonNull
    public static DataModel create(@NonNull PhotoJSONModel model) {
        return new PhotoDataModel(String.valueOf(model.id()), model.name(), model.description());
    }

    @NonNull private final String id;
    @NonNull private final String name;
    @NonNull private final String description;

    private PhotoDataModel(@NonNull String id, @NonNull String name, @NonNull String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
}
