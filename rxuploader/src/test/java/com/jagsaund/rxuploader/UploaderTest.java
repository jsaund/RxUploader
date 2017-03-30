package com.jagsaund.rxuploader;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.job.ErrorType;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.Status;
import com.jagsaund.rxuploader.rx.RxRequestBody;
import com.jagsaund.rxuploader.store.UploadService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MultipartBody;
import okio.BufferedSink;
import okio.Source;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import rx.Single;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UploaderTest {
    private static final String TEST_FILE = "upload_test.dat";

    @Mock private UploadErrorAdapter uploadErrorAdapter;

    @Test
    public void testUpload() throws Exception {
        final File file = getFile(TEST_FILE);
        final String jobId = "job-id";
        final Job job = Job.builder()
                .setId(jobId)
                .setStatus(Status.createQueued(jobId))
                .setMetadata(Collections.emptyMap())
                .setFilepath(file.getPath())
                .setMimeType("text/plain")
                .build();

        final BufferedSink sink = mock(BufferedSink.class);

        final List<Status> values = new ArrayList<>();
        values.add(Status.createSending(jobId, 0));

        final long total = file.length();
        long consumed = 0;
        while (consumed < total) {
            consumed = Math.min(total, consumed + RxRequestBody.BUFFER_SIZE);
            values.add(Status.createSending(jobId, (int) ((float) consumed * 100 / total)));
        }
        values.add(Status.createCompleted(jobId, "complete"));

        final Status[] expectedStatus = new Status[values.size()];
        values.toArray(expectedStatus);

        final UploadService service = (__, data) -> {
            try {
                data.body().writeTo(sink);
            } catch (@NonNull IOException e) {
                Single.error(e);
            }
            return Single.just("complete");
        };

        final Uploader uploader = new Uploader(service, uploadErrorAdapter, Schedulers.io());
        final TestSubscriber<Status> ts = TestSubscriber.create();
        uploader.upload(job, file).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValues(expectedStatus);
    }

    @Test
    public void testUploadFailed() throws Exception {
        final File file = getFile(TEST_FILE);
        final String jobId = "job-id";
        final Job job = Job.builder()
                .setId(jobId)
                .setStatus(Status.createQueued(jobId))
                .setMetadata(Collections.emptyMap())
                .setFilepath(file.getPath())
                .setMimeType("text/plain")
                .build();

        final List<Status> values = new ArrayList<>();
        values.add(Status.createSending(jobId, 0));

        final long total = file.length();
        long consumed = 0;
        while (consumed < total) {
            consumed = Math.min(total, consumed + RxRequestBody.BUFFER_SIZE);
            if (consumed > 0.5f * total) {
                values.add(Status.createFailed(jobId, ErrorType.UNKNOWN));
                break;
            }
            values.add(Status.createSending(jobId, (int) ((float) consumed * 100 / total)));
        }

        final Status[] expectedStatus = new Status[values.size()];
        values.toArray(expectedStatus);

        final BufferedSink sink = mock(BufferedSink.class);
        when(sink.write(any(Source.class), anyLong())).thenAnswer(new Answer<BufferedSink>() {
            long bytesRead = 0;

            @Override
            public BufferedSink answer(InvocationOnMock invocation) throws Throwable {
                bytesRead += (long) invocation.getArguments()[1];
                if (bytesRead > 0.5f * total) {
                    throw new IOException("exception");
                }
                return sink;
            }
        });

        when(uploadErrorAdapter.fromThrowable(any(IOException.class)))
                .thenReturn(ErrorType.UNKNOWN);

        final UploadService service = (__, data) -> {
            try {
                data.body().writeTo(sink);
            } catch (@NonNull IOException e) {
                return Single.error(e);
            }
            return Single.just("complete");
        };

        final Uploader uploader = new Uploader(service, uploadErrorAdapter, Schedulers.io());
        final TestSubscriber<Status> ts = TestSubscriber.create();
        uploader.upload(job, file).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValues(expectedStatus);
    }

    @Test
    public void testUploadFailedUnhandledException() throws Exception {
        final File file = getFile(TEST_FILE);
        final String jobId = "job-id";
        final Job job = Job.builder()
                .setId(jobId)
                .setStatus(Status.createQueued(jobId))
                .setMetadata(Collections.emptyMap())
                .setFilepath(file.getPath())
                .setMimeType("text/plain")
                .build();

        final List<Status> values = new ArrayList<>();
        values.add(Status.createFailed(jobId, ErrorType.UNKNOWN));

        final Status[] expectedStatus = new Status[values.size()];
        values.toArray(expectedStatus);

        when(uploadErrorAdapter.fromThrowable(any(RuntimeException.class)))
                .thenReturn(ErrorType.UNKNOWN);

        final UploadService service = mock(UploadService.class);
        when(service.upload(anyMap(), any(MultipartBody.Part.class)))
                .thenThrow(new RuntimeException(""));

        final Uploader uploader = new Uploader(service, uploadErrorAdapter, Schedulers.io());
        final TestSubscriber<Status> ts = TestSubscriber.create();
        uploader.upload(job, file).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValues(expectedStatus);
    }

    private File getFile(@NonNull String path) {
        final ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }
}