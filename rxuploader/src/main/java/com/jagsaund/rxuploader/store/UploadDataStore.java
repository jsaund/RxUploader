package com.jagsaund.rxuploader.store;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.Status;
import rx.Observable;

/**
 * A local store for persisting and retrieving {@link Job} items.
 */
public interface UploadDataStore {
    /**
     * Retrieves a single {@link Job} for the provided {@code jobId}.
     *
     * @param jobId id of the job to retrieve from the store
     * @return {@link Job} associated with the {@code jobId} or {@link Job#INVALID_JOB} if not found
     */
    @NonNull
    Observable<Job> get(@NonNull String jobId);

    /**
     * Retrieves all {@link Job} items.
     *
     * @return stream of {@link Job} observables - this can be none, one, or many
     */
    @NonNull
    Observable<Job> getAll();

    /**
     * Saves the provided {@link Job} to the local store.
     *
     * @param job the {@link Job} to persist
     * @return the persisted {@link Job} (typically should be the same unless mutated)
     */
    @NonNull
    Observable<Job> save(@NonNull Job job);

    /**
     * Update the status of a job based on the job id associated with the {@code status} item.
     *
     * @param status new status
     * @return updated {@link Job} or {@link Job#INVALID_JOB} if not found
     */
    @NonNull
    Observable<Job> update(@NonNull Status status);

    /**
     * Deletes the {@link Job} associated with the {@code jobId}.
     *
     * @param jobId {@link Job} to delete
     * @return the deleted {@link Job} or {@link Job#INVALID_JOB} if not found
     */
    @NonNull
    Observable<Job> delete(@NonNull String jobId);
}
