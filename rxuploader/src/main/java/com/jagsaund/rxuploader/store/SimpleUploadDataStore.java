package com.jagsaund.rxuploader.store;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.JobTypeAdapterFactory;
import com.jagsaund.rxuploader.job.Status;
import com.jagsaund.rxuploader.utils.StringUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * A local data store which persists {@link Job} items to {@link SharedPreferences}. {@link Job}
 * items are serialized/deserialized to/from JSON.
 */
public class SimpleUploadDataStore implements UploadDataStore {
    @VisibleForTesting static final String JOB_REPO = "upload_job_repository";

    @VisibleForTesting static final String KEY_JOB_IDS = "key_job_ids";

    @VisibleForTesting static final String KEY_JOB_ID_PREFIX = "key_job_";

    @NonNull private final Scheduler worker;
    @NonNull private final SharedPreferences sharedPreferences;
    @NonNull private final Gson gson;

    @VisibleForTesting
    @NonNull
    static String jobIdKey(@NonNull String jobId) {
        return KEY_JOB_ID_PREFIX + jobId;
    }

    @NonNull
    public static UploadDataStore create(@NonNull Context context) {
        final SharedPreferences sharedPreferences = context
                .getSharedPreferences(JOB_REPO, Context.MODE_PRIVATE);

        final HandlerThread thread = new HandlerThread("UploadDataStore");
        thread.start();

        final Scheduler worker = AndroidSchedulers.from(thread.getLooper());
        return new SimpleUploadDataStore(sharedPreferences, worker);
    }

    @VisibleForTesting
    SimpleUploadDataStore(@NonNull SharedPreferences sharedPreferences, @NonNull Scheduler worker) {
        this.sharedPreferences = sharedPreferences;
        this.worker = worker;
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(JobTypeAdapterFactory.create())
                .create();
    }

    @NonNull
    @Override
    public Observable<Job> get(@NonNull String jobId) {
        return Observable.fromCallable(() -> getJob(jobIdKey(jobId))).subscribeOn(worker);
    }

    @NonNull
    @Override
    public Observable<Job> getAll() {
        return Observable.fromCallable(new Callable<Set<Job>>() {
            @Override
            public Set<Job> call() throws Exception {
                final Set<String> jobIdKeys = sharedPreferences
                        .getStringSet(KEY_JOB_IDS, Collections.emptySet());
                if (jobIdKeys.isEmpty()) {
                    return Collections.emptySet();
                }

                final Set<Job> jobs = new HashSet<>(jobIdKeys.size());
                for (String key : jobIdKeys) {
                    jobs.add(getJob(key));
                }
                return jobs;
            }
        }).flatMap(Observable::from).subscribeOn(worker);
    }

    @SuppressLint({ "CommitPrefEdits", "ApplySharedPref" })
    @NonNull
    @Override
    public Observable<Job> save(@NonNull Job job) {
        return Observable.fromCallable(() -> {
            final String rawJob = gson.toJson(job);
            sharedPreferences.edit().putString(jobIdKey(job.id()), rawJob).commit();

            final Set<String> keys = sharedPreferences.getStringSet(KEY_JOB_IDS, new HashSet<>());
            keys.add(String.valueOf(job.id()));
            sharedPreferences.edit().putStringSet(KEY_JOB_IDS, keys).commit();

            return job;
        }).subscribeOn(worker);
    }

    @SuppressLint("CommitPrefEdits")
    @NonNull
    @Override
    public Observable<Job> update(@NonNull Status status) {
        return get(status.id())
                .filter(job -> !Job.isInvalid(job))
                .flatMap(job -> save(job.withStatus(status)))
                .defaultIfEmpty(Job.INVALID_JOB);
    }

    @SuppressLint({ "CommitPrefEdits", "ApplySharedPref" })
    @NonNull
    @Override
    public Observable<Job> delete(@NonNull String id) {
        return get(id)
                .filter(job -> !Job.isInvalid(job))
                .doOnNext(job -> {
                    sharedPreferences.edit().remove(jobIdKey(job.id())).commit();

                    final Set<String> keys = sharedPreferences
                            .getStringSet(KEY_JOB_IDS, Collections.emptySet());
                    keys.remove(jobIdKey(id));
                    sharedPreferences.edit().putStringSet(KEY_JOB_IDS, keys).commit();
                })
                .defaultIfEmpty(Job.INVALID_JOB);
    }

    @NonNull
    private Job getJob(@NonNull String key) throws JsonSyntaxException {
        final String rawJob = sharedPreferences.getString(key, null);
        if (StringUtils.isNullOrEmpty(rawJob)) {
            return Job.INVALID_JOB;
        }
        return gson.fromJson(rawJob, Job.class);
    }
}
