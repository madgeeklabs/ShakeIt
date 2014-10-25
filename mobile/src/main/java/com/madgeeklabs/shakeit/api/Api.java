package com.madgeeklabs.shakeit.api;

import android.content.res.TypedArray;

import java.util.List;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;

/**
 * Created by xa on 10/24/14.
 */
public interface Api {


    @POST("/api/readings")
    String readings(@Body List readings);


    @GET("/api/hello")
    String hello(@Header("pushId") String pushId);

}


