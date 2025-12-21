package com.particlesdevs.photoncamera.app;

import android.annotation.SuppressLint;
import android.content.Context;

@SuppressLint("StaticFieldLeak")
public class ContextProvider {

    private static Context context;

    public static void setContext(Context ctx) {
        context = ctx.getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
