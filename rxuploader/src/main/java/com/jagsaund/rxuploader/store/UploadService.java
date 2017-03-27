package com.jagsaund.rxuploader.store;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.job.Status;
import java.util.Map;
import okhttp3.MultipartBody;
import rx.Observable;

public interface UploadService {
    Observable<Status> upload(@NonNull Map<String, Object> metadata,
            @NonNull MultipartBody.Part data);
}
