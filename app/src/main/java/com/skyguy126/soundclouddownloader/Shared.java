package com.skyguy126.soundclouddownloader;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Shared {
    public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static final String PREFS_FILE_NAME = "scd_prefs";
    public static final String PREFS_CHECKBOX_KEY = "checkbox";
    public static final String PREFS_SPINNER_KEY = "spinner";
    public static final String SOURCE_LINK = "https://github.com/skyguy126/Xposed-SoundCloudDownloader";

    public static void openWebsite(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.android.chrome");
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            intent.setPackage(null);
            context.startActivity(intent);
        }
    }

    public static int getSpinnerPosition(String item) {
        switch (item) {
            case "SDCARD":
                return 0;
            case "DOWNLOAD":
                return 1;
            case "MUSIC":
                return 2;
            default:
                return -1;
        }
    }
}
