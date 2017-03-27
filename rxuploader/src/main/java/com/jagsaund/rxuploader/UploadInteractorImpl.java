package com.jagsaund.rxuploader;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.jagsaund.rxuploader.job.ErrorType;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.Status;
import com.jagsaund.rxuploader.store.UploadDataStore;
import java.io.File;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

class UploadInteractorImpl implements UploadInteractor {
    @NonNull private final Uploader uploader;
    @NonNull private final UploadDataStore uploadDataStore;
    @NonNull private final UploadErrorAdapter errorAdapter;

    @NonNull private final Scheduler networkScheduler;

    @NonNull
    static UploadInteractor create(@NonNull Uploader uploader, @NonNull UploadDataStore dataStore,
            @NonNull UploadErrorAdapter errorAdapter) {
        return new UploadInteractorImpl(uploader, dataStore, errorAdapter, Schedulers.io());
    }

    @VisibleForTesting
    UploadInteractorImpl(@NonNull Uploader uploader, @NonNull UploadDataStore dataStore,
            @NonNull UploadErrorAdapter errorAdapter, @NonNull Scheduler networkScheduler) {
        this.uploader = uploader;
        this.uploadDataStore = dataStore;
        this.errorAdapter = errorAdapter;
        this.networkScheduler = networkScheduler;
    }

    @NonNull
    @Override
    public Observable<Job> get(@NonNull String id) {
        return uploadDataStore.get(id);
    }

    @NonNull
    @Override
    public Observable<Job> getAll() {
        return uploadDataStore.getAll();
    }

    @NonNull
    @Override
    public Observable<Job> save(@NonNull Job job) {
        return uploadDataStore.save(job);
    }

    @NonNull
    @Override
    public Observable<Job> update(@NonNull Status status) {
        return uploadDataStore.update(status);
    }

    @NonNull
    @Override
    public Observable<Job> delete(@NonNull String id) {
        return uploadDataStore.delete(id);
    }

    @NonNull
    @Override
    public Observable<Status> upload(@NonNull String id) {
        return get(id)
                .filter(job -> !Job.isInvalid(job))
                .observeOn(networkScheduler)
                .flatMap(job -> {
                    final String jobId = job.id();
                    final File file = new File(job.filepath());
                    if (!file.exists()) {
                        final Status failed = Status.createFailed(jobId, ErrorType.FILE_NOT_FOUND);
                        return Observable.just(failed);
                    }
                    return uploader
                            .upload(job, file)
                            .distinctUntilChanged()
                            .onErrorResumeNext(error -> {
                                final ErrorType errorType = errorAdapter.fromThrowable(error);
                                final Status status = Status.createFailed(jobId, errorType);
                                return Observable.just(status);
                            });
                })
                .defaultIfEmpty(Status.createInvalid(id));
    }
}
