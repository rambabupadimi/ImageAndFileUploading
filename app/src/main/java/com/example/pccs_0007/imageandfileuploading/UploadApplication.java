package com.example.pccs_0007.imageandfileuploading;

import android.app.Application;

import com.androidnetworking.AndroidNetworking;

/**
 * Created by PCCS-0007 on 01-Mar-18.
 */

public class UploadApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidNetworking.initialize(getApplicationContext());
    }
}
