package com.jagsaund.rxuploader.sample.service;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.sample.model.wire.PhotosJSONModel;
import com.jagsaund.rxuploader.sample.model.wire.UploadPhotoJSONModel;
import okhttp3.MultipartBody;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import rx.Observable;

public interface ApiService {
    @NonNull
    @GET("v1/photos?feature=user")
    Observable<PhotosJSONModel> getPhotos(@Query("username") @NonNull String username);

    @NonNull
    @Multipart
    @POST("v1/photos/upload")
    Observable<UploadPhotoJSONModel> uploadPhoto(@Query("name") @NonNull String name,
            @Query("description") @NonNull String description, @Query("privacy") int privacy,
            @Part @NonNull MultipartBody.Part photo);
}
