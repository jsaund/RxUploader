package com.jagsaund.rxuploader.sample;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.jagsaund.rxuploader.sample.config.Config;
import com.jagsaund.rxuploader.sample.service.XOAuthProvider;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import rx.Completable;
import rx.Observable;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import timber.log.Timber;

public class SampleApplication extends Application {
    private static String token;
    private static String secret;

    @NonNull
    public static Completable authorize() {
        final OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(Config.HTTP_LOG_LEVEL);
        httpClient.addInterceptor(loggingInterceptor);

        final OkHttpOAuthConsumer consumer =
                new OkHttpOAuthConsumer(Config.CONSUMER_KEY, Config.CONSUMER_SECRET);

        final XOAuthProvider provider =
                new XOAuthProvider(Config.REQUEST_TOKEN_URL, Config.ACCESS_TOKEN_URL,
                        Config.AUTH_CALLBACK_URL, httpClient.build());

        return Observable.fromCallable(
                () -> provider.retrieveRequestToken(consumer, Config.AUTH_CALLBACK_URL))
                .flatMap(__ -> {
                    try {
                        provider.retrieveAccessToken(consumer, "");
                    } catch (@NonNull Exception e) {
                        Observable.error(e);
                    }
                    token = consumer.getToken();
                    secret = consumer.getTokenSecret();
                    return Observable.empty();
                })
                .toCompletable();
    }

    @Nullable
    public static String getToken() {
        return token;
    }

    @Nullable
    public static String getSecret() {
        return secret;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
