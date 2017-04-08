package com.jagsaund.rxuploader.sample;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.UploadErrorAdapter;
import com.jagsaund.rxuploader.job.ErrorType;
import java.io.FileNotFoundException;
import java.io.IOException;
import retrofit2.HttpException;

class PhotoUploadErrorAdapter implements UploadErrorAdapter {
    @NonNull
    @Override
    public ErrorType fromThrowable(@NonNull Throwable error) {
        if (error instanceof FileNotFoundException) {
            return ErrorType.FILE_NOT_FOUND;
        } else if (error instanceof HttpException) {
            return ErrorType.SERVICE;
        } else if (error instanceof IOException) {
            return ErrorType.NETWORK;
        }
        return ErrorType.UNKNOWN;
    }

    @Override
    public boolean canRetry(@NonNull Throwable error) {
        final ErrorType errorType = fromThrowable(error);
        return errorType != ErrorType.UNKNOWN && errorType != ErrorType.FILE_NOT_FOUND;
    }
}
