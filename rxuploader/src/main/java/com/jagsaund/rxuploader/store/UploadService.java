package com.jagsaund.rxuploader.store;

import android.support.annotation.NonNull;
import java.util.Map;
import okhttp3.MultipartBody;
import rx.Single;

/**
 * Uploads a multipart entity and optional metadata contents.
 *
 * @param <T> Defines the type of response payload returned by the upload operation
 */
public interface UploadService<T> {
    /**
     * Uploads multipart content to a remote endpoint.
     *
     * @param metadata Optional information to be associated with the upload operation
     * @param data A file to upload
     * @return The response received from the upload operation
     */
    Single<T> upload(@NonNull Map<String, Object> metadata, @NonNull MultipartBody.Part data);
}
