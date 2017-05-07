package com.jagsaund.rxuploader;

import com.jagsaund.rxuploader.job.ErrorType;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.Status;
import com.jagsaund.rxuploader.job.StatusType;
import java.io.IOException;
import java.util.Arrays;
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
import rx.subjects.TestSubject;

import static com.jagsaund.rxuploader.job.Status.createQueued;
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
            .setStatus(createQueued("job-id"))
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

    @Test
    public void testConcurrentUpload() throws Exception {
        // Expect uploads to be performed one at a time
        final String jobId1 = "job-id-1";
        final String jobId2 = "job-id-2";

        final Job job1 = Job.builder()
                .setId(jobId1)
                .setFilepath("filepath")
                .setMetadata(Collections.emptyMap())
                .setStatus(createQueued(jobId1))
                .setMimeType("text/plain")
                .build();

        final Job job2 = Job.builder()
                .setId(jobId2)
                .setFilepath("filepath")
                .setMetadata(Collections.emptyMap())
                .setStatus(createQueued(jobId2))
                .setMimeType("text/plain")
                .build();

        final Status[] statusesJob1 = new Status[]{
                Status.createSending(jobId1, 0),
                Status.createSending(jobId1, 20),
                Status.createSending(jobId1, 40),
                Status.createSending(jobId1, 60),
                Status.createSending(jobId1, 80),
                Status.createSending(jobId1, 100),
        };

        final Status[] statusesJob2 = new Status[]{
                Status.createSending(jobId2, 0),
                Status.createSending(jobId2, 20),
                Status.createSending(jobId2, 40),
                Status.createSending(jobId2, 60),
                Status.createSending(jobId2, 80),
                Status.createSending(jobId2, 100),
        };

        when(uploadInteractor.getAll())
                .thenReturn(Observable.from(Collections.emptyList()));

        when(uploadInteractor.save(job1))
                .thenReturn(Observable.just(job1));

        when(uploadInteractor.save(job2))
                .thenReturn(Observable.just(job2));

        when(uploadInteractor.update(job1.status()))
                .thenReturn(Observable.just(job1));

        when(uploadInteractor.update(job2.status()))
                .thenReturn(Observable.just(job2));

        // independently control execution of status emissions
        // simulate inherent delay of upload operation
        TestScheduler delayTestScheduler = new TestScheduler();
        when(uploadInteractor.upload(jobId1)).thenReturn(Observable.from(statusesJob1)
                .concatMap(s -> Observable.just(s)
                        .delay(50, TimeUnit.MILLISECONDS, delayTestScheduler)));

        // explicitly want the second upload job to emit items faster than the first
        // after all, we are trying to confirm that all the emissions from the first job will be
        // emitted before the second job begins
        when(uploadInteractor.upload(jobId2))
                .thenReturn(Observable.from(statusesJob2));

        final Status queuedJob1 = Status.createQueued(jobId1);
        final Status queuedJob2 = Status.createQueued(jobId2);

        // directly inject the two queued jobs -- these will propagate through the system to the
        // uploader
        statusSubject.onNext(queuedJob1);
        statusSubject.onNext(queuedJob2);

        final Status[] expected = new Status[statusesJob1.length + statusesJob2.length];
        System.arraycopy(statusesJob1, 0, expected, 0, statusesJob1.length);
        System.arraycopy(statusesJob2, 0, expected, statusesJob1.length, statusesJob2.length);

        // only interested in the sending progress updates
        final TestSubscriber<Status> ts = TestSubscriber.create();
        uploadManager.status()
                .filter(status -> status.statusType() == StatusType.SENDING)
                .subscribe(ts);

        testScheduler.triggerActions();

        // confirm that after the first delay we received just the first emission from job1
        delayTestScheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        delayTestScheduler.triggerActions();
        testScheduler.triggerActions();

        ts.assertValuesAndClear(expected[0]);

        // confirm that after the next set of delays, still only the first job is emitting status
        // updates
        delayTestScheduler.advanceTimeBy(50 * 4, TimeUnit.MILLISECONDS);
        delayTestScheduler.triggerActions();
        testScheduler.triggerActions();

        ts.assertValuesAndClear(expected[1], expected[2], expected[3], expected[4]);

        // complete the delay and verify that the last status is emitted from the first upload
        // operation and then receive the remaining status emissions from the 2nd upload
        delayTestScheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        delayTestScheduler.triggerActions();
        testScheduler.triggerActions();

        ts.assertValues(Arrays.copyOfRange(expected, 5, expected.length));
    }
}