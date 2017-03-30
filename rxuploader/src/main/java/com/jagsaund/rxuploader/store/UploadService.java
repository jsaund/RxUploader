package com.jagsaund.rxuploader.store;

import android.support.annotation.NonNull;
import java.util.Map;
import okhttp3.MultipartBody;
import rx.Single;

public interface UploadService<T> {
    Single<T> upload(@NonNull Map<String, Object> metadata, @NonNull MultipartBody.Part data);
}
