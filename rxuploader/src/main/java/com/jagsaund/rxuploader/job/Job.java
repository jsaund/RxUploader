package com.jagsaund.rxuploader.job;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import java.util.Collections;
import java.util.Map;

@AutoValue
public abstract class Job {
    public static final String INVALID_JOB_ID = "invalid_job_id";
    public static final Map<String, Object> INVALID_METADATA = Collections.emptyMap();
    public static final Job INVALID_JOB = Job.builder()
            .setId(INVALID_JOB_ID)
            .setStatus(Status.createInvalid(INVALID_JOB_ID))
            .setFilepath("")
            .setMetadata(INVALID_METADATA)
            .setMimeType("")
            .build();

    public static boolean isInvalid(@NonNull Job job) {
        return INVALID_JOB == job || INVALID_JOB_ID.equals(job.id());
    }

    @NonNull
    public static TypeAdapter<Job> typeAdapter(Gson gson) {
        return new AutoValue_Job.GsonTypeAdapter(gson);
    }

    @NonNull
    public static Builder builder() {
        return new AutoValue_Job.Builder();
    }

    @NonNull
    public abstract String id();

    @NonNull
    public abstract Status status();

    @NonNull
    public Job withStatus(@NonNull Status status) {
        return toBuilder().setStatus(status).build();
    }

    @NonNull
    abstract Builder toBuilder();

    @NonNull
    public abstract String filepath();

    @NonNull
    public abstract Map<String, Object> metadata();

    @NonNull
    public abstract String mimeType();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setId(String jobId);

        public abstract Builder setStatus(Status status);

        public abstract Builder setFilepath(String filepath);

        public abstract Builder setMetadata(Map<String, Object> metadata);

        public abstract Builder setMimeType(String mimeType);

        public abstract Job build();
    }
}
