package com.jagsaund.rxuploader.rx;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.jagsaund.rxuploader.job.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import rx.Emitter;

/**
 * Transforms a {@linkplain RequestBody} into one that is reactive and will emit progress updates
 * to the provide {@code Emitter}. Progress updates are emitted as {@linkplain Status} items.
 */
public class RxRequestBody extends RequestBody {
    public static final long BUFFER_SIZE = 8 * 1024;

    @NonNull private final String jobId;
    @NonNull private final MediaType mediaType;
    @NonNull private final InputStream inputStream;
    @NonNull private final Emitter<Status> progressEmitter;

    private final long length;

    @NonNull
    public static RequestBody create(@NonNull Emitter<Status> progressEmitter,
            @NonNull String jobId, @NonNull File file, @NonNull String mimeType)
            throws FileNotFoundException {
        final MediaType mediaType = MediaType.parse(mimeType);
        final InputStream inputStream = new FileInputStream(file);
        final long length = file.length();

        return new RxRequestBody(progressEmitter, jobId, mediaType, inputStream, length);
    }

    @VisibleForTesting
    RxRequestBody(@NonNull Emitter<Status> progressEmitter, @NonNull String jobId,
            @NonNull MediaType mediaType, @NonNull InputStream inputStream, long length) {
        this.progressEmitter = progressEmitter;
        this.jobId = jobId;
        this.mediaType = mediaType;
        this.inputStream = inputStream;
        this.length = length;
    }

    @NonNull
    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() {
        return length;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        try (final Source source = Okio.source(inputStream)) {
            final long total = contentLength();

            long remaining = total;
            long size = Math.min(BUFFER_SIZE, remaining);

            progressEmitter.onNext(Status.createSending(jobId, 0));

            while (remaining > 0) {
                sink.write(source, size);

                remaining = Math.max(0, remaining - size);
                size = Math.min(BUFFER_SIZE, remaining);

                final int progress = (int) (100 - ((float) remaining / total) * 100);
                progressEmitter.onNext(Status.createSending(jobId, progress));
            }
        }
    }
}
