package com.jagsaund.rxuploader;

import com.jagsaund.rxuploader.job.ErrorType;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.Status;
import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.schedulers.TestScheduler;
import rx.subjects.TestSubject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UploadManagerTest {
    private static final Job TEST_JOB = Job.builder()
            .setId("job-id")
            .setFilepath("filepath")
            .setMetadata(Collections.emptyMap())
            .setStatus(Status.createQueued("job-id"))
            .setMimeType("text/plain")
            .build();

    @Mock private UploadInteractor uploadInteractor;
    @Mock private UploadErrorAdapter uploadErrorAdapter;

    private TestScheduler testScheduler;
    private TestSubject<Status> statusSubject;
    private TestSubject<Job> jobSubject;
    private UploadManager uploadManager;

    @Before
    public void setUp() throws Exception {
        testScheduler = new TestScheduler();
        statusSubject = TestSubject.create(testScheduler);
        jobSubject = TestSubject.create(testScheduler);

        uploadManager =
                new UploadManager(uploadInteractor, uploadErrorAdapter, jobSubject, statusSubject);
    }

    @Test
    public void testEnqueue() throws Exception {
        when(uploadInteractor.save(TEST_JOB))
                .thenReturn(Observable.just(TEST_JOB));

        when(uploadInteractor.update(TEST_JOB.status()))
                .thenReturn(Observable.just(TEST_JOB));

        final Status completed = Status.createCompleted(TEST_JOB.id(), "Finished");
        when(uploadInteractor.update(completed))
                .thenReturn(Observable.just(TEST_JOB.withStatus(completed)));

        final Status[] statuses = new Status[] {
                Status.createSending(TEST_JOB.id(), 0),
                Status.createSending(TEST_JOB.id(), 50),
                Status.createSending(TEST_JOB.id(), 100),
                completed,
        };
        when(uploadInteractor.upload(TEST_JOB.id()))
                .thenReturn(Observable.from(statuses));

        when(uploadInteractor.delete(TEST_JOB.id()))
                .thenReturn(Observable.just(TEST_JOB.withStatus(completed)));

        // when a new job is queued to be uploaded
        uploadManager.enqueue(TEST_JOB);
        testScheduler.triggerActions();

        // it should first be saved to the job repository
        verify(uploadInteractor).save(TEST_JOB);
        // then the job should be passed to the status queue where the job status is updated
        verify(uploadInteractor).update(TEST_JOB.status());
        // then the status is filtered for queued items which should be sent to upload
        verify(uploadInteractor).upload(TEST_JOB.id());
        // then the status should be updated once upload completes
        verify(uploadInteractor).update(completed);
    }

    @Test
    public void testEnqueueUploadFailure() throws Exception {
        when(uploadErrorAdapter.fromThrowable(any(IOException.class)))
                .thenReturn(ErrorType.NETWORK);

        when(uploadInteractor.save(TEST_JOB))
                .thenReturn(Observable.just(TEST_JOB));

        when(uploadInteractor.update(TEST_JOB.status()))
                .thenReturn(Observable.just(TEST_JOB));

        final Status failed = Status.createFailed(TEST_JOB.id(), ErrorType.NETWORK);
        when(uploadInteractor.update(failed))
                .thenReturn(Observable.just(TEST_JOB.withStatus(failed)));

        final Status[] statuses = new Status[] {
                Status.createSending(TEST_JOB.id(), 0),
                Status.createSending(TEST_JOB.id(), 50),
        };
        when(uploadInteractor.upload(TEST_JOB.id()))
                .thenReturn(Observable.from(statuses)
                        .concatWith(Observable.error(new IOException())));

        // when a new job is queued to be uploaded
        uploadManager.enqueue(TEST_JOB);
        testScheduler.triggerActions();

        // it should first be saved to the job repository
        verify(uploadInteractor).save(TEST_JOB);
        // then the job should be passed to the status queue where the job status is updated
        verify(uploadInteractor).update(TEST_JOB.status());
        // then the status is filtered for queued items which should be sent to upload
        verify(uploadInteractor).upload(TEST_JOB.id());
        // then the status should be updated once upload failed
        verify(uploadInteractor).update(failed);

        // make sure the job was not deleted
        verify(uploadInteractor, times(0)).delete(anyString());
    }
}