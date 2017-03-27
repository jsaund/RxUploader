package com.jagsaund.rxuploader.store;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.Status;
import rx.Observable;

public interface UploadDataStore {
    @NonNull
    Observable<Job> get(@NonNull String jobId);

    @NonNull
    Observable<Job> getAll();

    @NonNull
    Observable<Job> save(@NonNull Job job);

    @NonNull
    Observable<Job> update(@NonNull Status status);

    @NonNull
    Observable<Job> delete(@NonNull String id);
}
