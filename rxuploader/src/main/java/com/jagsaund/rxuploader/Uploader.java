package com.jagsaund.rxuploader;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.Status;
import com.jagsaund.rxuploader.rx.RxRequestBody;
import com.jagsaund.rxuploader.store.UploadService;
import com.jagsaund.rxuploader.utils.StringUtils;
import java.io.File;
import java.io.FileNotFoundException;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

class Uploader {
    private static final String DEFAULT_FORM_DATA_NAME = "file";

    @NonNull private final UploadService uploadService;
    @NonNull private final Scheduler worker;

    @Nullable private String formDataName;

    @VisibleForTesting
    Uploader(@NonNull UploadService uploadService, @NonNull Scheduler worker) {
        this.uploadService = uploadService;
        this.worker = worker;
    }

    /**
     * Construct a new upload that will execute upload operations on the {@code Schedulers.io}
     * scheduler.
     *
     * @param uploadService Service to communicate to backend
     * @return A new uploader instance.
     */
    @NonNull
    static Uploader create(@NonNull UploadService uploadService) {
        return new Uploader(uploadService, Schedulers.io());
    }

    /**
     * Sets the name associated with the file form data part.
     * The default is {@value DEFAULT_FORM_DATA_NAME}.
     *
     * @param name Name associated with form data part.
     */
    public void setFormDataName(@NonNull String name) {
        formDataName = name;
    }

    /**
     * Uploads the file to the provided {@linkplain UploadService}.
     * Status updates (queued, sending, failed, completed) are communicated to the client as a
     * stream of {@linkplain Status} items.
     * The operation is performed on the IO Scheduler ({@code Schedulers.io}).
     *
     * @param job Provides information necessary to process an upload request
     * @param file The content to upload
     * @return An {@linkplain Observable} which emits {@linkplain Status} updates of the upload.
     */
    @NonNull
    public Observable<Status> upload(@NonNull Job job, @NonNull File file) {
        final String name = StringUtils.getOrDefault(formDataName, DEFAULT_FORM_DATA_NAME);
        return new UploadObservable(uploadService, job, file, name)
                .create()
                .subscribeOn(worker);
    }

    static class UploadObservable {
        @NonNull private final UploadService uploadService;
        @NonNull private final Job job;
        @NonNull private final File file;
        @NonNull private final String formDataName;

        UploadObservable(@NonNull UploadService uploadService, @NonNull Job job, @NonNull File file,
                @NonNull String formDataName) {
            this.uploadService = uploadService;
            this.job = job;
            this.file = file;
            this.formDataName = formDataName;
        }

        @NonNull
        Observable<Status> create() {
            return Observable.create(emitter -> {
                final RequestBody fileBody;
                final String jobId = job.id();
                try {
                    fileBody = RxRequestBody.create(emitter, jobId, file, job.mimeType());
                } catch(@NonNull FileNotFoundException e) {
                    emitter.onError(e);
                    return;
                }

                final String filename = file.getName();

                final MultipartBody.Part body =
                        MultipartBody.Part.createFormData(formDataName, filename, fileBody);
                final Subscription subscription = uploadService.upload(job.metadata(), body)
                        .subscribe(new SingleSubscriber() {
                            @Override
                            public void onSuccess(@NonNull Object response) {
                                emitter.onNext(Status.createCompleted(jobId, response));
                                emitter.onCompleted();
                            }

                            @Override
                            public void onError(@NonNull Throwable error) {
                                emitter.onError(error);
                            }
                        });
                emitter.setSubscription(subscription);
            }, Emitter.BackpressureMode.LATEST);
        }
    }
}
