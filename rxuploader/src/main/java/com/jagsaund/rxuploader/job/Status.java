package com.jagsaund.rxuploader.job;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.gson.annotations.Expose;

public class Status {
    @NonNull private final StatusType status;
    @NonNull private final String id;

    @Nullable private final ErrorType error;

    @Expose(serialize = false,
            deserialize = false) private final int progress;

    @Expose(serialize = false,
            deserialize = false) @Nullable private final Object response;

    private Status(@NonNull String id, @NonNull StatusType status, int progress,
            @Nullable ErrorType error, @Nullable Object response) {
        this.id = id;
        this.status = status;
        this.progress = progress;
        this.error = error;
        this.response = response;
    }

    @NonNull
    public static Status createQueued(@NonNull String id) {
        return new Status(id, StatusType.QUEUED, 0, null, null);
    }

    @NonNull
    public static Status createSending(@NonNull String id, int progress) {
        return new Status(id, StatusType.SENDING, progress, null, null);
    }

    @NonNull
    public static Status createCompleted(@NonNull String id, @Nullable Object response) {
        return new Status(id, StatusType.COMPLETED, 0, null, response);
    }

    @NonNull
    public static Status createFailed(@NonNull String id, @NonNull ErrorType error) {
        return new Status(id, StatusType.FAILED, 0, error, null);
    }

    @NonNull
    public static Status createInvalid(@NonNull String id) {
        return new Status(id, StatusType.INVALID, 0, null, null);
    }

    @NonNull
    public String id() {
        return id;
    }

    @NonNull
    public StatusType statusType() {
        return status;
    }

    public int progress() {
        return progress;
    }

    @NonNull
    public Status withProgress(int progress) {
        return createSending(id, progress);
    }

    @Nullable
    public ErrorType error() {
        return error;
    }

    @Nullable
    public Object response() {
        return response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Status jobStatus = (Status) o;

        if (progress != jobStatus.progress) return false;
        if (status != jobStatus.status) return false;
        if (error != jobStatus.error) return false;
        if (!id.equals(jobStatus.id)) return false;
        return response != null ? response.equals(jobStatus.response) : jobStatus.response == null;
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + (error != null ? error.hashCode() : 0);
        result = 31 * result + id.hashCode();
        result = 31 * result + progress;
        result = 31 * result + (response != null ? response.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JobStatus{"
                + "status="
                + status
                + ", id="
                + id
                + ", progress="
                + progress
                + ", error="
                + error
                + ", response="
                + response
                + '}';
    }
}
