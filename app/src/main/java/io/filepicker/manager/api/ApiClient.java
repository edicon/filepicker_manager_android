package io.filepicker.manager.api;

import android.content.Context;

import io.filepicker.manager.models.File;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by maciejwitowski on 11/3/14.
 */
public class ApiClient {

    private static final String BASE_URL = "https://www.filepicker.io";
    private static final String API_PATH = "/api";
    private static final String FILE_PATH = API_PATH + "/file";

    private static ApiInterface apiInterface;

    public static ApiInterface getApiClient() {
        if (apiInterface == null) {
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(BASE_URL)
                    .build();


            apiInterface = restAdapter.create(ApiInterface.class);
        }
        return apiInterface;
    }

    public interface ApiInterface {

        @FormUrlEncoded
        @POST(API_PATH + "/store/S3")
        void storeFile(@Query("key") String apikey,
                       @Field("url") String url,
                       Callback<File> file);

        @Headers("Content-Type: */*; charset=binary")
        @GET(FILE_PATH + "/{id}")
        void getFilelinkContent(@Path("id") String id,
                                Callback<Response> objectCallback);

    }
}
