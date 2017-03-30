package com.jagsaund.rxuploader;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.job.ErrorType;

public interface UploadErrorAdapter {
    @NonNull
    ErrorType fromThrowable(@NonNull Throwable error);

    boolean canRetry(@NonNull Throwable error);
}
