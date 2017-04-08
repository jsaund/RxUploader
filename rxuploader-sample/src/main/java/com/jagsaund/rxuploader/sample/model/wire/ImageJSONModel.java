package com.jagsaund.rxuploader.sample.model.wire;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class ImageJSONModel {
    @NonNull
    public static TypeAdapter<ImageJSONModel> typeAdapter(Gson gson) {
        return new AutoValue_ImageJSONModel.GsonTypeAdapter(gson);
    }

    @SerializedName("size")
    public abstract Integer size();

    @SerializedName("url")
    public abstract String url();
}
