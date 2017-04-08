package com.jagsaund.rxuploader.sample.service;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.jagsaund.rxuploader.sample.model.wire.PhotoJSONModel;
import com.jagsaund.rxuploader.sample.model.wire.UploadPhotoJSONModel;
import com.jagsaund.rxuploader.store.UploadService;
import java.util.Map;
import okhttp3.MultipartBody;
import rx.Single;

public class PhotoUploadService implements UploadService<PhotoJSONModel> {
    @NonNull private final ApiService apiService;

    @VisibleForTesting
    PhotoUploadService(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    @NonNull
    public static UploadService create(@NonNull ApiService apiService) {
        return new PhotoUploadService(apiService);
    }

    @Override
    public Single<PhotoJSONModel> upload(@NonNull Map<String, Object> metadata,
            @NonNull MultipartBody.Part data) {
        final String name = (String) metadata.get("name");
        final String description = (String) metadata.get("description");
        final Integer privacy = (Integer) metadata.get("privacy");

        return apiService
                .uploadPhoto(name, description, privacy != null ? privacy : 1, data)
                .map(UploadPhotoJSONModel::photo)
                .toSingle();
    }
}
