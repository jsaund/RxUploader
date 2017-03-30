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
import java.io.File;
import rx.Observable;
import rx.functions.Actions;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;

/**
 * Manages uploading content to a remote endpoint along with {@linkplain Job} persistence. Uploads
 * are queued as {@linkplain Job} items. Progress updates are communicated to the client as
 * {@linkplain Status} items.
 * <br/>
 * Constructing a new {@linkplain UploadManager} is done by using the {@linkplain Builder}.
 * <br/>
 * A new {@linkplain Job} can be executed by using the {@code enqueue} method.
 */
public class UploadManager {
    @NonNull private final UploadInteractor uploadInteractor;

    /**
     * New {@link Job} items to be added are inserted to this subject. Items are persisted to the
     * {@link UploadDataStore} and added to the {@code statusSubject} to be processed for uploads.
     */
    @NonNull private final Subject<Job, Job> jobSubject;

    /**
     * Communicates status between clients and business logic.
     */
    @NonNull private final Subject<Status, Status> statusSubject;

    /**
     * Consumes from {@code statusSubject} ready to be shared with clients.
     */
    @NonNull private final Observable<Status> statusObservable;

    @NonNull private final CompositeSubscription subscriptions;

    @VisibleForTesting
    UploadManager(@NonNull UploadInteractor uploadInteractor,
            @NonNull UploadErrorAdapter errorAdapter, @NonNull Subject<Job, Job> jobSubject,
            @NonNull Subject<Status, Status> statusSubject) {
        this.jobSubject = jobSubject;
        this.statusSubject = statusSubject;

        this.uploadInteractor = uploadInteractor;

        subscriptions = new CompositeSubscription();

        // read items from the job subject
        // save them to the data store
        // enqueue items in to the status subject for processing
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

        // update the status of incoming status items read from the status subject
        final ConnectableObservable<Status> statusUpdates = statusSubject
                .asObservable()
                .filter(this::canUpdateStatus)
                .flatMap(uploadInteractor::update)
                .map(Job::status)
                .publish();

        // consume items that have status type of queued and upload them
        final Observable<Status> uploadJobs = statusUpdates
                .filter(status -> status.statusType() == StatusType.QUEUED)
                .flatMap(status -> uploadInteractor.upload(status.id()));

        // consume items that have status type of completed and delete them from the data store
        // and the original file from disk
        final Observable<Boolean> deleteJobs = statusUpdates
                .filter(status -> status.statusType() == StatusType.COMPLETED)
                .flatMap(status -> uploadInteractor.delete(status.id()))
                .map(job -> {
                    final File file = new File(job.filepath());
                    return file.exists() && file.delete();
                });

        // status updates which are progress updates of how much has been uploaded can be too
        // much for the client to consume -- filter this out and apply a backpressure mode
        // to keep the latest
        final Observable<Status> sending = statusUpdates
                .filter(status -> status.statusType() == StatusType.SENDING)
                .onBackpressureLatest();

        // filter out the remaining type of updates
        final Observable<Status> updates = statusUpdates
                .filter(status -> {
                    final StatusType type = status.statusType();
                    return type != StatusType.SENDING && type != StatusType.INVALID;
                });

        // merge the sending and remaining updates with the backpressure modes applied
        // this will be used to share with clients
        statusObservable = updates.mergeWith(sending).share();

        subscriptions.add(jobQueue.subscribe(statusSubject::onNext));
        subscriptions.add(uploadJobs.subscribe(statusSubject::onNext));
        subscriptions.add(deleteJobs.subscribe(Actions.empty()));

        subscriptions.add(statusUpdates.connect());
    }

    /**
     * Receive status updates of upload progress.
     *
     * @return A stream of {@link Status} items
     */
    @NonNull
    public Observable<Status> status() {
        final Observable<Status> persistedStatus = uploadInteractor
                .getAll()
                .map(Job::status);

        return persistedStatus.concatWith(statusObservable);
    }

    /**
     * Enqueues a new {@link Job}
     */
    public void enqueue(@NonNull Job job) {
        jobSubject.onNext(job);
    }

    /**
     * Retry a specific job
     */
    public void retry(@NonNull String jobId) {
        final Observable<Status> observable = uploadInteractor
                .get(jobId)
                .filter(this::canRetry)
                .map(job -> Status.createQueued(job.id()));
        subscriptions.add(observable.subscribe(statusSubject::onNext));
    }

    /**
     * Retry all failed jobs
     */
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

    /**
     * Builder used to construct a new {@link UploadManager} instance.
     */
    public static class Builder {
        private UploadService uploadService;
        private UploadDataStore uploadDataStore;
        private UploadErrorAdapter uploadErrorAdapter;

        private Builder() {
        }

        /**
         * Upload service used to upload content to a remote endpoint
         * Required
         *
         * @param uploadService upload's content to a remote endpoint
         * @return Builder
         */
        public Builder withUploadService(@NonNull UploadService uploadService) {
            this.uploadService = uploadService;
            return this;
        }

        /**
         * Define the data store to persist {@link Job} items.
         * Required (or use {@code withSimpleUploadDataStore})
         *
         * @param uploadDataStore Data store to persist jobs
         * @return Builder
         */
        public Builder withUploadDataStore(@NonNull UploadDataStore uploadDataStore) {
            this.uploadDataStore = uploadDataStore;
            return this;
        }

        /**
         * Use the {@link SimpleUploadDataStore} data store to persist jobs.
         *
         * @return Builder
         */
        public Builder withSimpleUploadDataStore(@NonNull Context context) {
            this.uploadDataStore = SimpleUploadDataStore.create(context);
            return this;
        }

        /**
         * Defines the adapter to translate exceptions to {@link ErrorType}.
         * Required
         *
         * @param uploadErrorAdapter Map exceptions to {@link ErrorType}
         * @return Builder
         */
        public Builder withUploadErrorAdapter(@NonNull UploadErrorAdapter uploadErrorAdapter) {
            this.uploadErrorAdapter = uploadErrorAdapter;
            return this;
        }

        @NonNull
        public UploadManager build() {
            if (uploadService == null) {
                throw new IllegalArgumentException("Must provide a valid upload service");
            }

            if (uploadDataStore == null) {
                throw new IllegalArgumentException("Must provide a valid upload data store");
            }

            if (uploadErrorAdapter == null) {
                throw new IllegalArgumentException("Must provide a valid upload error adapter");
            }

            final Subject<Job, Job> jobSubject = BehaviorSubject.<Job>create().toSerialized();
            final Subject<Status, Status> statusSubject =
                    BehaviorSubject.<Status>create().toSerialized();

            final Uploader uploader = Uploader.create(uploadService, uploadErrorAdapter);
            final UploadInteractor uploadInteractor =
                    UploadInteractorImpl.create(uploader, uploadDataStore, uploadErrorAdapter);

            return new UploadManager(uploadInteractor, uploadErrorAdapter, jobSubject,
                    statusSubject);
        }
    }
}
