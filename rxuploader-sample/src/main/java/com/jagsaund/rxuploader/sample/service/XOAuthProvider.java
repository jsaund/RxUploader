package com.jagsaund.rxuploader.sample.service;

import com.jagsaund.rxuploader.sample.config.Config;
import oauth.signpost.AbstractOAuthProvider;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.http.HttpResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Util;
import se.akerfeldt.okhttp.signpost.OkHttpRequestAdapter;
import se.akerfeldt.okhttp.signpost.OkHttpResponseAdapter;

public class XOAuthProvider extends AbstractOAuthProvider {

    private transient OkHttpClient okHttpClient;

    public XOAuthProvider(String requestTokenEndpointUrl, String accessTokenEndpointUrl,
                               String authorizationWebsiteUrl) {
        super(requestTokenEndpointUrl, accessTokenEndpointUrl, authorizationWebsiteUrl);
        this.okHttpClient = new OkHttpClient();
    }

    public XOAuthProvider(String requestTokenEndpointUrl, String accessTokenEndpointUrl,
                               String authorizationWebsiteUrl, OkHttpClient okHttpClient) {
        super(requestTokenEndpointUrl, accessTokenEndpointUrl, authorizationWebsiteUrl);
        this.okHttpClient = okHttpClient;
    }

    public void setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    @Override
    protected HttpRequest createRequest(String endpointUrl) throws Exception {
        final Request.Builder builder =
                new Request.Builder()
                        .url(endpointUrl)
                        .post(Util.EMPTY_REQUEST);
        if (endpointUrl.contains(Config.ACCESS_TOKEN_URL)) {
            final MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            final String body =
                    String.format("x_auth_mode=%s&x_auth_password=%s&x_auth_username=%s",
                            Config.X_AUTH_MODE, Config.X_AUTH_PASSWORD, Config.X_AUTH_USERNAME);
            builder.post(RequestBody.create(mediaType, body));
        }
        return new OkHttpRequestAdapter(builder.build());
    }

    @Override
    protected HttpResponse sendRequest(HttpRequest request) throws Exception {
        Response response = okHttpClient.newCall((Request) request.unwrap()).execute();
        return new OkHttpResponseAdapter(response);
    }
}
