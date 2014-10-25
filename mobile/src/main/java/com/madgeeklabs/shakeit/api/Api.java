package com.madgeeklabs.shakeit.api;

import android.content.res.TypedArray;

import java.util.List;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.mime.TypedFile;

/**
 * Created by xa on 10/24/14.
 */
public interface Api {


        @Multipart
    @POST("/api/uploadPicture")
    void uploadSelfie(@Header("username") String username,
                      @Part("fileUpload") TypedFile fileUpload, Callback<Response> cb);

    @POST("/api/readings")
    String readings(@Body List readings);


    @GET("/api/hello")
    String hello(@Header("pushId") String pushId);

}


