package com.jagsaund.rxuploader.sample.model.wire;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import java.util.List;

@AutoValue
public abstract class PhotosJSONModel {
    @NonNull
    public static TypeAdapter<PhotosJSONModel> typeAdapter(Gson gson) {
        return new AutoValue_PhotosJSONModel.GsonTypeAdapter(gson);
    }

    @SerializedName("photos")
    public abstract List<PhotoJSONModel> photos();
}
