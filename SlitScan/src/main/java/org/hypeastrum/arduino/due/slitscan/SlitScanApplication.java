package org.hypeastrum.arduino.due.slitscan;

import android.app.Application;
import android.content.Context;

public class SlitScanApplication extends Application {
    private static SlitScanApplication APPLICATION_INSTANCE;

    private StatusCenter statusCenter;

    public static Context context() {
        return instance().getApplicationContext();
    }

    public static SlitScanApplication instance() {
        return APPLICATION_INSTANCE;
    }

    public StatusCenter getStatusCenter() {
        return statusCenter;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        APPLICATION_INSTANCE = this;
        init();
    }

    private void init() {
        statusCenter = new StatusCenter();
    }
}
