package com.jagsaund.rxuploader.store;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jagsaund.rxuploader.BuildConfig;
import com.jagsaund.rxuploader.job.ErrorType;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.JobTypeAdapterFactory;
import com.jagsaund.rxuploader.job.Status;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class , sdk = 23)
public class SimpleUploadDataStoreTest {
    private static final String TEST_JOB_ID  = "test_job_id";

    private SharedPreferences sharedPreferences;
    private SimpleUploadDataStore dataStore;
    private Gson gson;

    @Before
    public void setUp() throws Exception {
        sharedPreferences =
                RuntimeEnvironment.application.getSharedPreferences(SimpleUploadDataStore.JOB_REPO,
                        Context.MODE_PRIVATE);
        final Scheduler worker = Schedulers.from(Executors.newSingleThreadExecutor());
        dataStore = new SimpleUploadDataStore(sharedPreferences, worker);
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(JobTypeAdapterFactory.create())
                .create();
    }

    @SuppressLint("ApplySharedPref")
    @After
    public void tearDown() throws Exception {
        sharedPreferences.edit().clear().commit();
    }

    @Test
    public void testJobIdKey() throws Exception {
        final String expectedJobIdKey = SimpleUploadDataStore.KEY_JOB_ID_PREFIX + TEST_JOB_ID;
        assertThat(SimpleUploadDataStore.jobIdKey(TEST_JOB_ID), is(expectedJobIdKey));
    }

    @SuppressLint("ApplySharedPref")
    @Test
    public void testGet() throws Exception {
        final Job test = createTestJob();
        final String json = gson.toJson(test);
        final String key = SimpleUploadDataStore.jobIdKey(test.id());
        final Set<String> keys = new HashSet<>();
        keys.add(key);

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(SimpleUploadDataStore.KEY_JOB_IDS, keys);
        editor.putString(key, json);
        editor.commit();

        final TestSubscriber<Job> ts = TestSubscriber.create();
        dataStore.get(test.id()).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValueCount(1);
        ts.assertValue(test);
    }

    @SuppressLint("ApplySharedPref")
    @Test
    public void testGetInvalidJobId() throws Exception {
        final Job test = createTestJob();
        final String json = gson.toJson(test);
        final String key = SimpleUploadDataStore.jobIdKey(test.id());
        final Set<String> keys = new HashSet<>();
        keys.add(key);

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(SimpleUploadDataStore.KEY_JOB_IDS, keys);
        editor.putString(key, json);
        editor.commit();

        final TestSubscriber<Job> ts = TestSubscriber.create();
        dataStore.get("bad_id").subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValueCount(1);
        ts.assertValue(Job.INVALID_JOB);
    }

    @SuppressLint("ApplySharedPref")
    @Test
    public void testGetAll() throws Exception {
        final Job job1 = Job.builder()
                .setId("job_id_1")
                .setFilepath("test/file/path/1")
                .setMetadata(Collections.emptyMap())
                .setMimeType("text/plain")
                .setStatus(Status.createQueued("job_id_1"))
                .build();

        final Job job2 = Job.builder()
                .setId("job_id_2")
                .setFilepath("test/file/path/2")
                .setMetadata(Collections.emptyMap())
                .setMimeType("text/plain")
                .setStatus(Status.createCompleted("job_id_2", null))
                .build();

        final Job job3 = Job.builder()
                .setId("job_id_3")
                .setFilepath("test/file/path/3")
                .setMetadata(Collections.emptyMap())
                .setMimeType("text/plain")
                .setStatus(Status.createFailed("job_id_3", ErrorType.SERVICE))
                .build();

        final Set<String> keys = new HashSet<>();
        keys.add(SimpleUploadDataStore.jobIdKey(job1.id()));
        keys.add(SimpleUploadDataStore.jobIdKey(job2.id()));
        keys.add(SimpleUploadDataStore.jobIdKey(job3.id()));

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(SimpleUploadDataStore.KEY_JOB_IDS, keys);
        editor.putString(SimpleUploadDataStore.jobIdKey(job1.id()), gson.toJson(job1));
        editor.putString(SimpleUploadDataStore.jobIdKey(job2.id()), gson.toJson(job2));
        editor.putString(SimpleUploadDataStore.jobIdKey(job3.id()), gson.toJson(job3));
        editor.commit();

        final TestSubscriber<Job> ts = TestSubscriber.create();
        dataStore.getAll().subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValueCount(3);

        final List<Job> jobs = ts.getOnNextEvents();
        assertThat(jobs, containsInAnyOrder(job1, job2, job3));
    }

    @Test
    public void testGetAllEmpty() throws Exception {
        final TestSubscriber<Job> ts = TestSubscriber.create();
        dataStore.getAll().subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertCompleted();
    }

    @Test
    public void save() throws Exception {
        final Job job = Job.builder()
                .setId("job_id_1")
                .setFilepath("test/file/path/1")
                .setMetadata(Collections.emptyMap())
                .setMimeType("text/plain")
                .setStatus(Status.createQueued("job_id_1"))
                .build();

        final TestSubscriber<Job> ts = TestSubscriber.create();
        dataStore.save(job).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValueCount(1);
        ts.assertValue(job);

        final TestSubscriber<Job> ts2 = TestSubscriber.create();
        dataStore.getAll().subscribe(ts2);

        ts2.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts2.assertNoErrors();
        ts2.assertCompleted();
        ts2.assertValueCount(1);
        ts2.assertValue(job);

    }

    @Test
    public void update() throws Exception {
        final Job test = createTestJob();
        final String json = gson.toJson(test);
        final String key = SimpleUploadDataStore.jobIdKey(test.id());
        final Set<String> keys = new HashSet<>();
        keys.add(key);

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(SimpleUploadDataStore.KEY_JOB_IDS, keys);
        editor.putString(key, json);
        editor.apply();

        final TestSubscriber<Job> ts = TestSubscriber.create();
        dataStore.update(Status.createCompleted(test.id(), null)).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValueCount(1);
        ts.assertValue(test.withStatus(Status.createCompleted(test.id(), null)));
    }

    @SuppressLint("ApplySharedPref")
    @Test
    public void delete() throws Exception {
        final Job test = createTestJob();
        final String json = gson.toJson(test);
        final String key = SimpleUploadDataStore.jobIdKey(test.id());
        final Set<String> keys = new HashSet<>();
        keys.add(key);

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(SimpleUploadDataStore.KEY_JOB_IDS, keys);
        editor.putString(key, json);
        editor.commit();

        final TestSubscriber<Job> ts = TestSubscriber.create();
        dataStore.delete(test.id()).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValueCount(1);
        ts.assertValue(test);

        final TestSubscriber<Job> ts2 = TestSubscriber.create();
        dataStore.getAll().subscribe(ts2);

        ts2.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts2.assertNoErrors();
        ts2.assertCompleted();
        ts2.assertNoValues();
    }

    private Job createTestJob() {
        // all numeric values are serialized as doubles by gson
        final Map<String, Object> data = new HashMap<>();
        data.put("key1", "string");
        data.put("key2", 2.0);

        return Job.builder()
                .setId(TEST_JOB_ID)
                .setFilepath("test/file/path.dat")
                .setMetadata(data)
                .setMimeType("text/plain")
                .setStatus(Status.createQueued(TEST_JOB_ID))
                .build();
    }
}