package com.jagsaund.rxuploader.sample.config;

import okhttp3.logging.HttpLoggingInterceptor;

public class Config {
    public static final String HOST = "https://api.500px.com/";

    public static final HttpLoggingInterceptor.Level HTTP_LOG_LEVEL =
            HttpLoggingInterceptor.Level.HEADERS;


    public static final String REQUEST_TOKEN_URL = HOST + "v1/oauth/request_token";
    public static final String ACCESS_TOKEN_URL = HOST + "v1/oauth/access_token";
    public static final String AUTH_CALLBACK_URL = "";

    public static final String CONSUMER_KEY = "<INSERT YOUR 500PX CONSUMER KEY>";
    public static final String CONSUMER_SECRET = "<INSERT YOUR 500PX CONSUMER SECRET>";

    public static final String X_AUTH_MODE = "client_auth";
    public static final String X_AUTH_USERNAME = "<INSERT YOUR 500PX USERNAME>";
    public static final String X_AUTH_PASSWORD = "<INSERT YOUR 500PX PASSWORD>";

    public static final String USERNAME = X_AUTH_USERNAME;
}
