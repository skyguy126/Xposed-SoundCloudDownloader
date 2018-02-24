package com.skyguy126.soundclouddownloader;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class Downloader {

    private static boolean validatePermissions(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                alert.setTitle("Permission Error");
                alert.setMessage("Grant permissions in the next dialog and retry download.");
                alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    }
                });
                alert.show();
                return false;
            }
        } else {
            return true;
        }
    }

    public static void download(DownloadPayload downloadPayload) {
        final File file = new File(downloadPayload.getSaveDirectory(), downloadPayload.getFileName());
        final String filePath = file.getPath();

        XposedBridge.log("[SoundCloud Downloader] Download path: " + filePath);

        if (file.exists()) {
            Toast.makeText(XposedMod.currentActivity, "File already exists!", Toast.LENGTH_SHORT).show();
            return;
        }

        final DownloadManager downloadManager = (DownloadManager) XposedMod.currentActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadPayload.getUrl()));
        final IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        request.setTitle(downloadPayload.getTitle());
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(Uri.fromFile(file));

        try {
            if (downloadPayload.includeMetadata()) {
                XposedBridge.log("[SoundCloud Downloader] Adding metadata injector");
                XposedMod.currentActivity.registerReceiver(new MetadataInjector(downloadPayload), filter);
            }

            downloadManager.enqueue(request);
            Toast.makeText(XposedMod.currentActivity, "Downloading...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            XposedBridge.log("[SoundCloud Downloader] Download Error: " + e.getMessage());
            Toast.makeText(XposedMod.currentActivity, "Download failed!", Toast.LENGTH_SHORT).show();
        }
    }

    public static DownloadPayload buildDownloadPayload(String url, String title, String imgUrl, String artistName, String genre) {
        if (!validatePermissions(XposedMod.currentActivity))
            return null;

        final String fileName = Shared.validateFileName(title + ".mp3");
        File saveDirectory;

        XposedBridge.log("[SoundCloud Downloader] " + Shared.PACKAGE_NAME + " " + Shared.PREFS_FILE_NAME);

        XSharedPreferences prefs = new XSharedPreferences(Shared.PACKAGE_NAME, Shared.PREFS_FILE_NAME);
        boolean includeMetadata = prefs.getBoolean(Shared.PREFS_METADATA_KEY, false);
        XposedBridge.log("[SoundCloud Downloader] include metadata: " + includeMetadata);

        String saveLocationString = prefs.getString(Shared.PREFS_SPINNER_KEY, "-1");
        int saveLocation = Integer.parseInt(saveLocationString);
        XposedBridge.log("[SoundCloud Downloader] saveloc: " + saveLocation);

        if (saveLocation == 0) {
            saveDirectory = Environment.getExternalStorageDirectory();
        } else if (saveLocation == 1) {
            saveDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else if (saveLocation == 2){
            saveDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        } else {
            saveDirectory = new File(prefs.getString(Shared.PREFS_SAVE_PATH_KEY, Shared.CUSTOM_PATH_DEFAULT));
        }

        return new DownloadPayload(url, fileName, saveDirectory, title, artistName, genre, imgUrl, includeMetadata);
    }
}
