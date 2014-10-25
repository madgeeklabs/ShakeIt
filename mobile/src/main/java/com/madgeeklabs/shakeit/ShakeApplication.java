package com.madgeeklabs.shakeit;

import android.app.Application;
import android.content.Context;


/**
 * Created by goofyahead on 10/25/14.
 */
public class ShakeApplication extends Application {
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return mContext;
    }


}
