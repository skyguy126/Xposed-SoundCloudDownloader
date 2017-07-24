package com.skyguy126.soundclouddownloader;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedMod implements IXposedHookLoadPackage {

    private static int saveLoc;
    private static Object urlBuilder;
    private static volatile Activity currentActivity;

    private static void download(Context context, String url, String name) {

        if (!Shared.validatePermissions(currentActivity))
            return;

        String fileName = Shared.validateFileName(name + ".mp3");

        File saveLoc;

        if (XposedMod.saveLoc == 0)
            saveLoc = Environment.getExternalStorageDirectory();
        else if (XposedMod.saveLoc == 1)
            saveLoc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        else
            saveLoc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

        File file = new File(saveLoc, fileName);

        XposedBridge.log("[SoundCloud Downloader] Download path: " + file.getPath());

        if (file.exists()) {
            Toast.makeText(context, "File already exists!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show();

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(Uri.fromFile(file));

        downloadManager.enqueue(request);
    }

    private static void addDownloadItem(Object popupMenuWrapper) {
        Object popupMenu = XposedHelpers.getObjectField(popupMenuWrapper, "popupMenu");
        Object menu = XposedHelpers.callMethod(popupMenu, "getMenu");
        XposedHelpers.callMethod(menu, "add", "Download");
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("com.soundcloud.android"))
            return;

        XposedBridge.log("[SoundCloud Downloader] Entry");

        XSharedPreferences prefs = new XSharedPreferences(Shared.PACKAGE_NAME, Shared.PREFS_FILE_NAME);
        XposedMod.saveLoc = prefs.getInt(Shared.PREFS_SPINNER_KEY, 1);

        XC_MethodHook downloadCatcher = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                MenuItem item = (MenuItem) param.args[0];
                if (!item.getTitle().toString().equalsIgnoreCase("Download"))
                    return;

                Object track = XposedHelpers.getObjectField(param.thisObject, "track");
                Object urn = XposedHelpers.callMethod(track, "getUrn");

                String className = track.getClass().getName();
                String name;

                if (className.equals("com.soundcloud.android.playback.ui.PlayerTrackState"))
                    name = (String) XposedHelpers.callMethod(track, "getTitle");
                else
                    name = (String) XposedHelpers.callMethod(track, "title");

                if (urlBuilder != null) {
                    String url = (String) XposedHelpers.callMethod(urlBuilder, "buildHttpsStreamUrl", new Class[]{XposedHelpers.findClass("com.soundcloud.android.model.Urn", lpparam.classLoader)}, urn);
                    XposedMod.download(currentActivity, url, name);
                } else {
                    Toast.makeText(currentActivity, "Failed to get url!", Toast.LENGTH_SHORT).show();
                }

                param.setResult(true);
            }
        };

        try {

            XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "newActivity", ClassLoader.class, String.class, Intent.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedMod.currentActivity = (Activity) param.getResult();
                }
            });

            XposedHelpers.findAndHookMethod("android.app.Activity", lpparam.classLoader, "onRequestPermissionsResult", int.class, String[].class, int[].class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    XposedBridge.log("[SoundCloud Downloader] got result");
                    return null;
                }
            });

            XposedHelpers.findAndHookConstructor("com.soundcloud.android.playback.StreamUrlBuilder", lpparam.classLoader, XposedHelpers.findClass("com.soundcloud.android.accounts.AccountOperations", lpparam.classLoader), XposedHelpers.findClass("com.soundcloud.android.api.ApiUrlBuilder", lpparam.classLoader), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedMod.urlBuilder = param.thisObject;
                }
            });

            XposedHelpers.findAndHookMethod("com.soundcloud.android.playback.ui.TrackPageMenuController", lpparam.classLoader, "setupMenu", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedMod.addDownloadItem(XposedHelpers.getObjectField(param.thisObject, "popupMenuWrapper"));
                }
            });

            XposedHelpers.findAndHookMethod("com.soundcloud.android.tracks.TrackItemMenuPresenter", lpparam.classLoader, "setupMenu", android.view.View.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedMod.addDownloadItem(param.getResult());
                }
            });

            XposedHelpers.findAndHookMethod("com.soundcloud.android.playback.ui.TrackPageMenuController", lpparam.classLoader, "onMenuItemClick", android.view.MenuItem.class, android.content.Context.class, downloadCatcher);
            XposedHelpers.findAndHookMethod("com.soundcloud.android.tracks.TrackItemMenuPresenter", lpparam.classLoader, "onMenuItemClick", android.view.MenuItem.class, android.content.Context.class, downloadCatcher);

        } catch (Throwable t) {
            XposedBridge.log("[SoundCloud Downloader] Error: " + t.getMessage());
            return;
        }

        XposedBridge.log("[SoundCloud Downloader] Initialized");
    }
}
