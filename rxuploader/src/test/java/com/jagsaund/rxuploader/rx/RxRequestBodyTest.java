package com.jagsaund.rxuploader.rx;

import com.jagsaund.rxuploader.job.Status;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Source;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Emitter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RxRequestBodyTest {

    @Test
    public void testWrite() throws Exception {
        final String jobId = "test-job-id";
        final long length = RxRequestBody.BUFFER_SIZE * 3;
        final BufferedSink sink = mock(BufferedSink.class);
        final InputStream inputStream = mock(InputStream.class);
        final Emitter<Status> emitter = mock(Emitter.class);

        final RequestBody requestBody =
                new RxRequestBody(emitter, jobId, MediaType.parse("text/plain"), inputStream,
                        length);

        requestBody.writeTo(sink);

        verify(sink, times(3)).write(any(Source.class), anyLong());

        verify(emitter).onNext(Status.createSending(jobId, 0));
        verify(emitter).onNext(Status.createSending(jobId, 33));
        verify(emitter).onNext(Status.createSending(jobId, 66));
        verify(emitter).onNext(Status.createSending(jobId, 100));
    }

    @Test
    public void testWriteEmpty() throws Exception {
        final String jobId = "test-job-id";
        final long length = 0;
        final BufferedSink sink = mock(BufferedSink.class);
        final InputStream inputStream = mock(InputStream.class);
        final Emitter<Status> emitter = mock(Emitter.class);

        final RequestBody requestBody =
                new RxRequestBody(emitter, jobId, MediaType.parse("text/plain"), inputStream,
                        length);

        requestBody.writeTo(sink);

        verify(sink, times(0)).write(any(Source.class), anyLong());

        verify(emitter).onNext(Status.createSending(jobId, 0));
    }

    @Test
    public void testWriteSmall() throws Exception {
        final String jobId = "test-job-id";
        final long length = 1;
        final BufferedSink sink = mock(BufferedSink.class);
        final InputStream inputStream = mock(InputStream.class);
        final Emitter<Status> emitter = mock(Emitter.class);

        final RequestBody requestBody =
                new RxRequestBody(emitter, jobId, MediaType.parse("text/plain"), inputStream,
                        length);

        requestBody.writeTo(sink);

        verify(sink, times(1)).write(any(Source.class), anyLong());

        verify(emitter).onNext(Status.createSending(jobId, 0));
        verify(emitter).onNext(Status.createSending(jobId, 100));
    }

    @Test(expected = IOException.class)
    public void testWriteError() throws Exception {
        final String jobId = "test-job-id";
        final long length = RxRequestBody.BUFFER_SIZE * 3;
        final BufferedSink sink = mock(BufferedSink.class);
        final InputStream inputStream = mock(InputStream.class);
        final Emitter<Status> emitter = mock(Emitter.class);

        final RequestBody requestBody =
                new RxRequestBody(emitter, jobId, MediaType.parse("text/plain"), inputStream,
                        length);

        when(sink.write(any(Source.class), anyInt())).thenThrow(new IOException("Error"));

        // expect this to throw an IOException
        requestBody.writeTo(sink);

        verify(sink, times(3)).write(any(Source.class), anyLong());
        verify(emitter).onNext(Status.createSending(jobId, 0));
    }

    @Test
    public void testContentLength() throws Exception {
        final String jobId = "test-job-id";
        final long length = RxRequestBody.BUFFER_SIZE * 3;
        final InputStream inputStream = mock(InputStream.class);
        final Emitter<Status> emitter = mock(Emitter.class);
        final RequestBody requestBody =
                new RxRequestBody(emitter, jobId, MediaType.parse("text/plain"), inputStream,
                        length);

        assertThat(requestBody.contentLength(), is(length));
    }

    @Test
    public void testContentType() throws Exception {
        final String jobId = "test-job-id";
        final long length = RxRequestBody.BUFFER_SIZE * 3;
        final InputStream inputStream = mock(InputStream.class);
        final Emitter<Status> emitter = mock(Emitter.class);
        final MediaType mediaType = MediaType.parse("text/plain");
        final RequestBody requestBody =
                new RxRequestBody(emitter, jobId, mediaType, inputStream, length);

        assertThat(requestBody.contentType(), is(mediaType));
    }
}
