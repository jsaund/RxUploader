package com.jagsaund.rxuploader;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.job.ErrorType;

/**
 * Defines how the {@link Uploader} should handle errors. This includes mapping {@link Throwable} to
 * {@link ErrorType} and determining which errors can be retried.
 */
public interface UploadErrorAdapter {
    /**
     * Maps {@link Throwable} to an {@link ErrorType}.
     *
     * @param error The error to map from
     * @return an {@link ErrorType} representing the received {@code error}
     */
    @NonNull
    ErrorType fromThrowable(@NonNull Throwable error);

    /**
     * Determines if a particular {@code error} can be retried.
     *
     * @param error check if the error can be retried
     * @return {@link Boolean#TRUE} if the job can be retried and {@link Boolean#FALSE} otherwise
     */
    boolean canRetry(@NonNull Throwable error);
}
