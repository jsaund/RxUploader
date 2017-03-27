package com.jagsaund.rxuploader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.jagsaund.rxuploader.job.ErrorType;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.Status;
import com.jagsaund.rxuploader.job.StatusType;
import com.jagsaund.rxuploader.store.SimpleUploadDataStore;
import com.jagsaund.rxuploader.store.UploadDataStore;
import com.jagsaund.rxuploader.store.UploadService;
import rx.Observable;
import rx.functions.Actions;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;

public class UploadManager {
    @NonNull private final UploadInteractor uploadInteractor;

    @NonNull private final Subject<Job, Job> jobSubject;
    @NonNull private final Subject<Status, Status> statusSubject;
    @NonNull private final Observable<Status> statusObservable;

    @NonNull private final CompositeSubscription subscriptions;

    @VisibleForTesting
    UploadManager(@NonNull UploadService uploadService, @NonNull UploadDataStore uploadDataStore,
            @NonNull UploadErrorAdapter errorAdapter, @NonNull Subject<Job, Job> jobSubject,
            @NonNull Subject<Status, Status> statusSubject) {
        this.jobSubject = jobSubject;
        this.statusSubject = statusSubject;

        final Uploader uploader = Uploader.create(uploadService, errorAdapter);
        this.uploadInteractor = UploadInteractorImpl
                .create(uploader, uploadDataStore, errorAdapter);

        subscriptions = new CompositeSubscription();

        final Observable<Status> jobQueue = jobSubject.asObservable()
                .filter(job -> job.status().statusType() == StatusType.QUEUED)
                .flatMap(job -> uploadInteractor
                        .save(job)
                        .onErrorResumeNext(error -> {
                            final String jobId = job.id();
                            final ErrorType errorType = errorAdapter.fromThrowable(error);
                            final Status status = Status.createFailed(jobId, errorType);
                            return Observable.just(job.withStatus(status));
                        }))
                .map(Job::status);

        final ConnectableObservable<Status> statusUpdates = statusSubject
                .asObservable()
                .filter(this::canUpdateStatus)
                .flatMap(uploadInteractor::update)
                .map(Job::status)
                .publish();

        final Observable<Status> uploadJobs = statusUpdates
                .filter(status -> status.statusType() == StatusType.QUEUED)
                .flatMap(status -> uploadInteractor.upload(status.id()));

        final Observable<Boolean> deleteJobs = statusUpdates
                .filter(status -> status.statusType() == StatusType.COMPLETED)
                .flatMap(status -> uploadInteractor.delete(status.id()))
                .map(__ -> Boolean.TRUE);

        final Observable<Status> sending = statusUpdates
                .filter(status -> status.statusType() == StatusType.SENDING)
                .onBackpressureDrop();

        final Observable<Status> updates = statusUpdates
                .filter(status -> {
                    final StatusType type = status.statusType();
                    return type != StatusType.SENDING && type != StatusType.INVALID;
                });

        statusObservable = updates.mergeWith(sending);

        subscriptions.add(jobQueue.subscribe(statusSubject::onNext));
        subscriptions.add(uploadJobs.subscribe(statusSubject::onNext));
        subscriptions.add(deleteJobs.subscribe(Actions.empty()));

        subscriptions.add(statusUpdates.connect());
    }

    @NonNull
    public Observable<Status> status() {
        final Observable<Status> persistedStatus = uploadInteractor
                .getAll()
                .map(Job::status);

        final Observable<Status> status = statusObservable.share();

        return persistedStatus.concatWith(status);
    }

    public void enqueue(@NonNull Job job) {
        jobSubject.onNext(job);
    }

    public void retry(@NonNull String jobId) {
        final Observable<Status> observable = uploadInteractor
                .get(jobId)
                .filter(this::canRetry)
                .map(job -> Status.createQueued(job.id()));
        subscriptions.add(observable.subscribe(statusSubject::onNext));
    }

    public void retryAll() {
        final Observable<Status> observable = uploadInteractor
                .getAll()
                .filter(this::canRetry)
                .map(job -> Status.createQueued(job.id()));
        subscriptions.add(observable.subscribe(statusSubject::onNext));
    }

    private boolean canRetry(@NonNull Job job) {
        final Status status = job.status();
        return status.statusType() == StatusType.FAILED && status.error() != ErrorType.UNKNOWN;
    }

    private boolean canUpdateStatus(@NonNull Status status) {
        final StatusType statusType = status.statusType();
        return statusType == StatusType.COMPLETED
                || statusType == StatusType.FAILED
                || statusType == StatusType.QUEUED;
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UploadService uploadService;
        private UploadDataStore uploadDataStore;
        private UploadErrorAdapter uploadErrorAdapter;

        private Builder() {
        }

        public Builder withUploadService(@NonNull UploadService uploadService) {
            this.uploadService = uploadService;
            return this;
        }

        public Builder withUploadDataStore(@NonNull UploadDataStore uploadDataStore) {
            this.uploadDataStore = uploadDataStore;
            return this;
        }

        public Builder withSimpleUploadDataStore(@NonNull Context context) {
            this.uploadDataStore = SimpleUploadDataStore.create(context);
            return this;
        }

        public Builder withUploadErrorAdapter(@NonNull UploadErrorAdapter uploadErrorAdapter) {
            this.uploadErrorAdapter = uploadErrorAdapter;
            return this;
        }

        @NonNull
        public UploadManager build() {
            final Subject<Job, Job> jobSubject = BehaviorSubject.<Job>create().toSerialized();
            final Subject<Status, Status> statusSubject =
                    BehaviorSubject.<Status>create().toSerialized();

            if (uploadService == null) {
                throw new IllegalArgumentException("Must provide a valid upload service");
            }

            if (uploadDataStore == null) {
                throw new IllegalArgumentException("Must provide a valid upload data store");
            }

            if (uploadErrorAdapter == null) {
                throw new IllegalArgumentException("Must provide a valid upload error adapter");
            }

            return new UploadManager(uploadService, uploadDataStore, uploadErrorAdapter, jobSubject,
                    statusSubject);
        }
    }
}
