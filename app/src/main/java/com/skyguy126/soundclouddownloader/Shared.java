package com.skyguy126.soundclouddownloader;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

public class Shared {
    public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static final String PREFS_FILE_NAME = "scd_prefs";
    public static final String PREFS_LAUNCHER_ICON_KEY = "disp_launcher_icon_checkbox";
    public static final String PREFS_SPINNER_KEY = "save_loc_spinner";
    public static final String PREFS_SAVE_PATH_KEY = "save_loc_spinner_path";
    public static final String PREFS_METADATA_KEY = "include_metadata";
    public static final String SOURCE_LINK = "https://github.com/skyguy126/Xposed-SoundCloudDownloader";
    public static final String CUSTOM_PATH_DEFAULT = "/sdcard/";

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

    public static String getSpinnerDescription(int key) {
        switch (key) {
            case 0:
                return "/sdcard/";
            case 1:
                return "/sdcard/Download/";
            case 2:
                return "/sdcard/Music/";
            case 3:
                return "User Defined";
            default:
                return "";
        }
    }

    public static String validateFileName(String fileName) {
        return fileName.replaceAll("[|\\\\?*<\":>+/]", "");
    }

    public static boolean validatePermissions(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                alert.setTitle("Permission Error");
                alert.setMessage("Grant permissions in the next dialog and retry download.");
                alert.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(activity, new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                });
                alert.show();
                return false;
            }
        } else {
            return true;
        }
    }
}
