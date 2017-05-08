package com.jagsaund.rxuploader.sample;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;
import com.jagsaund.rxuploader.UploadManager;
import com.jagsaund.rxuploader.job.Job;
import com.jagsaund.rxuploader.job.Status;
import com.jagsaund.rxuploader.job.StatusType;
import com.jagsaund.rxuploader.sample.config.Config;
import com.jagsaund.rxuploader.sample.model.data.DataModel;
import com.jagsaund.rxuploader.sample.model.data.PhotoDataModel;
import com.jagsaund.rxuploader.sample.model.data.PhotoPrivacy;
import com.jagsaund.rxuploader.sample.model.data.UploadDataModel;
import com.jagsaund.rxuploader.sample.model.wire.PhotoJSONModel;
import com.jagsaund.rxuploader.sample.service.ApiService;
import com.jagsaund.rxuploader.sample.service.PhotoUploadService;
import com.jagsaund.rxuploader.sample.service.Service;
import com.jagsaund.rxuploader.utils.StringUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private static final String[] FILENAME = new String[]{"test-1.jpg", "test-2.jpg", "test-3.jpg"};
    private static final String[] NAME = new String[]{"test-1", "test-2", "test-3"};
    private static final String[] DESCRIPTION = new String[]{"test-1", "test-2", "test-3"};

    // photo will not be marked as a public upload
    private static final PhotoPrivacy PRIVACY_MODE = PhotoPrivacy.PRIVATE;

    private ApiService apiService;
    private UploadRecyclerAdapter adapter;
    private UploadManager uploadManager;
    private CompositeSubscription subscriptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Timber.tag("RxUploader-MainActivity");

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final String token = SampleApplication.getToken();
        final String secret = SampleApplication.getSecret();
        if (StringUtils.isNullOrEmpty(token) || StringUtils.isNullOrEmpty(secret)) {
            Toast.makeText(this, "Invalid or empty token / secret", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        subscriptions = new CompositeSubscription();

        adapter = new UploadRecyclerAdapter(this);
        final RecyclerView jobsListView = (RecyclerView) findViewById(R.id.list_jobs);
        jobsListView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        jobsListView.setAdapter(adapter);

        final View uploadButton = findViewById(R.id.button_upload_photos);
        uploadButton.setOnClickListener(v -> {
            for (int i = 0; i < FILENAME.length; i++) {
                final String jobId = String.valueOf(System.currentTimeMillis());
                final String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
                final File file = new File(externalStoragePath, FILENAME[i]);
                final Map<String, Object> metadata = new HashMap<>();
                metadata.put("description", DESCRIPTION[i]);
                metadata.put("name", NAME[i]);
                metadata.put("privacy", PRIVACY_MODE.value);

                final Job job = Job.builder()
                        .setId(jobId)
                        .setStatus(Status.createQueued(jobId))
                        .setFilepath(file.getPath())
                        .setMetadata(metadata)
                        .setMimeType("image/jpeg")
                        .build();

                uploadManager.enqueue(job);
            }
        });

        apiService = Service.apiService(token, secret);
        uploadManager = UploadManager.builder()
                .withSimpleUploadDataStore(this)
                .withUploadService(PhotoUploadService.create(apiService))
                .withUploadErrorAdapter(new PhotoUploadErrorAdapter())
                .build();

        subscriptions.add(uploadManager.status()
                .flatMap(status -> {
                    final String jobId = status.id();
                    return uploadManager.getJob(jobId)
                            .filter(job -> !Job.isInvalid(job))
                            .map(job -> job.withStatus(status));
                })
                .map(job -> {
                    final Map<String, Object> metadata = job.metadata();
                    final String name = StringUtils.getOrEmpty((String) metadata.get("name"));
                    final String description = StringUtils
                            .getOrEmpty((String) metadata.get("description"));
                    return UploadDataModel.create(name, description, job.status());
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(model -> {
                    final Status status = model.getStatus();
                    if (status.statusType() == StatusType.COMPLETED) {
                        adapter.remove(model);
                        if (status.response() instanceof PhotoJSONModel) {
                            final PhotoJSONModel response = (PhotoJSONModel) status.response();
                            if (response != null) {
                                adapter.add(PhotoDataModel.create(response));
                            }
                        }
                    } else {
                        adapter.add(model);
                    }
        }));

        getPhotos();
    }

    private void getPhotos() {
        final Subscription getPhotos = apiService.getPhotos(Config.USERNAME)
                .map(response -> {
                    final List<PhotoJSONModel> photos = response.photos();
                    final int count = photos.size();
                    final List<DataModel> result = new ArrayList<>(count);
                    for (PhotoJSONModel p : photos) {
                        result.add(PhotoDataModel.create(p));
                    }
                    return result;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adapter::addAll, error -> Timber.e(error, "Failed to get photos"));

        subscriptions.add(getPhotos);
    }
}
