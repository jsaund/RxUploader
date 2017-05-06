package com.jagsaund.rxuploader;

import android.support.annotation.NonNull;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.Status;
import com.jagsaund.rxuploader.store.UploadDataStore;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UploadInteractorImplTest {
    private static final String TEST_FILE = "upload_test.dat";

    @Mock private Uploader uploader;
    @Mock private UploadDataStore dataStore;
    @Mock private UploadErrorAdapter errorAdapter;

    private TestScheduler testScheduler;
    private UploadInteractorImpl uploadInteractor;

    @Before
    public void setUp() throws Exception {
        testScheduler = new TestScheduler();
        uploadInteractor =
                new UploadInteractorImpl(uploader, dataStore, errorAdapter, testScheduler);
    }

    @Test
    public void testUpload() throws Exception {
        final String jobId = "job-id";
        final File file = getFile(TEST_FILE);
        final Job job = Job.builder()
                .setId(jobId)
                .setFilepath(file.getPath())
                .setMetadata(Collections.emptyMap())
                .setMimeType("text/plain")
                .setStatus(Status.createQueued(jobId))
                .build();

        when(dataStore.get(jobId)).thenReturn(Observable.just(job));

        final Status[] statuses = new Status[] {
                Status.createSending(jobId, 0), Status.createSending(jobId, 10),
                Status.createSending(jobId, 20), Status.createSending(jobId, 30),
                Status.createSending(jobId, 40), Status.createSending(jobId, 50),
                Status.createSending(jobId, 60), Status.createSending(jobId, 70),
                Status.createSending(jobId, 80), Status.createSending(jobId, 90),
                Status.createSending(jobId, 100), Status.createCompleted(jobId, "Finished"),
        };

        when(uploader.upload(job, file)).thenReturn(Observable.from(statuses));

        final TestSubscriber<Status> ts = TestSubscriber.create();
        uploadInteractor.upload(jobId).subscribe(ts);

        testScheduler.triggerActions();

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValues(statuses);
    }

    @Test
    public void testUploadJobNotFound() throws Exception {
        final String jobId = "job-id";
        when(dataStore.get(jobId)).thenReturn(Observable.just(Job.INVALID_JOB));

        final TestSubscriber<Status> ts = TestSubscriber.create();
        uploadInteractor.upload(jobId).subscribe(ts);

        testScheduler.triggerActions();

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValueCount(1);
        ts.assertValue(Status.createInvalid(jobId));

        verify(uploader, times(0)).upload(any(Job.class), any(File.class));
    }

    @Test
    public void testUploadFileNotFound() throws Exception {
        final String jobId = "job-id";
        final Job job = Job.builder()
                .setId(jobId)
                .setFilepath("invalid")
                .setMetadata(Collections.emptyMap())
                .setMimeType("text/plain")
                .setStatus(Status.createQueued(jobId))
                .build();

        when(dataStore.get(jobId)).thenReturn(Observable.just(job));

        final TestSubscriber<Status> ts = TestSubscriber.create();
        uploadInteractor.upload(jobId).subscribe(ts);

        testScheduler.triggerActions();

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertError(FileNotFoundException.class);
        ts.assertNoValues();

        verify(uploader, times(0)).upload(any(Job.class), any(File.class));
    }

    @Test
    public void testUploadError() throws Exception {
        final String jobId = "job-id";
        final File file = getFile(TEST_FILE);
        final Job job = Job.builder()
                .setId(jobId)
                .setFilepath(file.getPath())
                .setMetadata(Collections.emptyMap())
                .setMimeType("text/plain")
                .setStatus(Status.createQueued(jobId))
                .build();

        when(dataStore.get(jobId)).thenReturn(Observable.just(job));

        final Status[] statuses = new Status[] {
                Status.createSending(jobId, 0), Status.createSending(jobId, 10),
                Status.createSending(jobId, 20), Status.createSending(jobId, 30),
        };

        when(uploader.upload(eq(job), any(File.class))).thenReturn(
                Observable.from(statuses).concatWith(Observable.error(new IOException("error"))));

        final TestSubscriber<Status> ts = TestSubscriber.create();
        uploadInteractor.upload(jobId).subscribe(ts);

        testScheduler.triggerActions();

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);

        ts.assertValues(statuses);
        ts.assertError(IOException.class);
    }

    private File getFile(@NonNull String path) {
        final ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }
}