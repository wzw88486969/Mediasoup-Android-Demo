package com.example.mediasoupdemo;

import android.app.Application;

import org.mediasoup.droid.MediasoupClient;

public class MyApp extends Application {

    private static MyApp instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        MediasoupClient.initialize(getApplicationContext());
    }

    public static MyApp getInstance() {
        return instance;
    }
}
