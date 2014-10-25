package com.madgeeklabs.shakeit;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by goofyahead on 9/10/14.
 */
public class ShakeitSharedPrefs {

    private static final String REGISTRATION_ID = "REGISTRATIOND_ID";

    private final SharedPreferences prefs;

    public ShakeitSharedPrefs(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public void setRegistrationPushId(String regid) {
        prefs.edit().putString(REGISTRATION_ID, regid).commit();
    }

    public String getRegistrationPushId() {
        return prefs.getString(REGISTRATION_ID, "");
    }

}
