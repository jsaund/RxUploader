package com.jagsaund.rxuploader.sample.model.wire;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import java.util.List;

@AutoValue
public abstract class PhotoJSONModel {
    @NonNull
    public static TypeAdapter<PhotoJSONModel> typeAdapter(Gson gson) {
        return new AutoValue_PhotoJSONModel.GsonTypeAdapter(gson);
    }

    @SerializedName("id")
    public abstract Integer id();

    @SerializedName("user_id")
    public abstract Integer userId();

    @SerializedName("name")
    public abstract String name();

    @SerializedName("description")
    public abstract String description();

    @Nullable
    @SerializedName("width")
    public abstract Integer width();

    @Nullable
    @SerializedName("height")
    public abstract Integer height();

    @SerializedName("image_url")
    public abstract String imageUrl();

    @SerializedName("images")
    public abstract List<ImageJSONModel> images();

    @SerializedName("user")
    public abstract UserJSONModel user();
}
