package com.jagsaund.rxuploader.sample.model.wire;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class UploadPhotoJSONModel {
    @NonNull
    public static TypeAdapter<UploadPhotoJSONModel> typeAdapter(Gson gson) {
        return new AutoValue_UploadPhotoJSONModel.GsonTypeAdapter(gson);
    }

    @SerializedName("photo")
    public abstract PhotoJSONModel photo();
}
