package com.jagsaund.rxuploader.sample.service;

import android.support.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    @NonNull private final String authToken;

    AuthInterceptor(@NonNull final String authToken) {
        this.authToken = authToken;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        final Request original = chain.request();
        final Request authRequest = original.newBuilder()
                .addHeader("Authorization", "Bearer " + authToken)
                .method(original.method(), original.body())
                .build();
        return chain.proceed(authRequest);
    }
}
