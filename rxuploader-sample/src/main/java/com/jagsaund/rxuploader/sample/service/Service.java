package com.jagsaund.rxuploader.sample.service;

import android.support.annotation.NonNull;
import com.google.gson.GsonBuilder;
import com.jagsaund.rxuploader.sample.config.Config;
import com.jagsaund.rxuploader.sample.model.wire.JSONModelTypeAdapterFactory;
import java.util.HashMap;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import se.akerfeldt.okhttp.signpost.SigningInterceptor;

public final class Service {
    private static final Map<Class<?>, Object> serviceMap = new HashMap<>();

    private Service() {
    }

    @NonNull
    public static ApiService apiService(@NonNull String token, @NonNull String secret) {
        ApiService s = (ApiService) serviceMap.get(ApiService.class);
        if (s == null) {
            s = createService(ApiService.class, token, secret);
            serviceMap.put(ApiService.class, s);
        }
        return s;
    }

    @NonNull
    private static <S> S createService(@NonNull Class<S> s, @NonNull String token,
            @NonNull String secret) {
        final GsonConverterFactory serializer = GsonConverterFactory.create(
                new GsonBuilder().registerTypeAdapterFactory(JSONModelTypeAdapterFactory.create())
                        .create());

        final OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        OkHttpOAuthConsumer consumer = new OkHttpOAuthConsumer(
                Config.CONSUMER_KEY, Config.CONSUMER_SECRET);
        consumer.setTokenWithSecret(token, secret);
        httpClient.addInterceptor(new SigningInterceptor(consumer));

        final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(Config.HTTP_LOG_LEVEL);
        httpClient.addInterceptor(loggingInterceptor);

        final Retrofit client = new Retrofit.Builder().baseUrl(Config.HOST)
                .addConverterFactory(serializer)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(httpClient.build())
                .build();
        return client.create(s);
    }
}
