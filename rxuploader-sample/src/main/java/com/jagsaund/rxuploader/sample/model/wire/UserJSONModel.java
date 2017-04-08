package com.jagsaund.rxuploader.sample.model.wire;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class UserJSONModel {
    @NonNull
    public static TypeAdapter<UserJSONModel> typeAdapter(Gson gson) {
        return new AutoValue_UserJSONModel.GsonTypeAdapter(gson);
    }

    @SerializedName("id")
    public abstract int id();

    @SerializedName("username")
    public abstract String username();

    @SerializedName("firstname")
    public abstract String firstname();

    @SerializedName("lastname")
    public abstract String lastname();

    @SerializedName("city")
    public abstract String city();

    @SerializedName("country")
    public abstract String country();

    @SerializedName("fullname")
    public abstract String fullname();

    @SerializedName("userpic_url")
    public abstract String userpicUrl();

    @SerializedName("upgrade_status")
    public abstract Integer upgradeStatus();
}
