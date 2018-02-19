package com.skyguy126.soundclouddownloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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

    //TODO - will cause memory leak, find workaround
    private static volatile Activity currentActivity;
    private static Object urlBuilder;

    private static void download(Context context, String url, File saveDirectory, String fileName) {
        File file = new File(saveDirectory, fileName);

        XposedBridge.log("[SoundCloud Downloader] Download path: " + file.getPath());

        if (file.exists()) {
            Toast.makeText(context, "File already exists!", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(Uri.fromFile(file));

        try {
            downloadManager.enqueue(request);
            Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            XposedBridge.log("[SoundCloud Downloader] Download Error: " + e.getMessage());
            Toast.makeText(context, "Download failed!", Toast.LENGTH_SHORT).show();
        }
    }

    private static void buildDownload(final Context context, final String url, final String name) {

        if (!Shared.validatePermissions(currentActivity))
            return;

        final String fileName = Shared.validateFileName(name + ".mp3");
        File saveDirectory;

        XSharedPreferences prefs = new XSharedPreferences(Shared.PACKAGE_NAME, Shared.PREFS_FILE_NAME);
        int saveLocation = prefs.getInt(Shared.PREFS_SPINNER_KEY, -1);

        if (saveLocation == 0) {
            saveDirectory = Environment.getExternalStorageDirectory();
        } else if (saveLocation == 1) {
            saveDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else if (saveLocation == 2){
            saveDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Save Directory");

            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
            input.setText(Shared.CUSTOM_PATH_DEFAULT);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    File saveDirectory = new File(input.getText().toString());
                    XposedMod.download(context, url, saveDirectory, fileName);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
            return;
        }

        XposedMod.download(context, url, saveDirectory, fileName);
    }

    private static void addDownloadItem(Object popupMenuWrapper) {
        Object popupMenu = XposedHelpers.getObjectField(popupMenuWrapper, "a");
        Object menu = XposedHelpers.callMethod(popupMenu, "getMenu");
        XposedHelpers.callMethod(menu, "add", "Download");
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("com.soundcloud.android"))
            return;

        XposedBridge.log("[SoundCloud Downloader] Entry");

        XC_MethodHook downloadCatcher = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                MenuItem item = (MenuItem) param.args[0];
                if (!item.getTitle().toString().equalsIgnoreCase("Download"))
                    return;

                String currentClassName = param.thisObject.getClass().getName();
                String name;
                Object urn;

                if (currentClassName.equals("exo")) {
                    Object track = XposedHelpers.getObjectField(param.thisObject, "l");
                    urn = XposedHelpers.callMethod(track, "getUrn");
                    name = (String) XposedHelpers.callMethod(track, "d");
                } else {
                    Object track = XposedHelpers.getObjectField(param.thisObject, "r");
                    urn = XposedHelpers.callMethod(track, "getUrn");
                    name = (String) XposedHelpers.callMethod(track, "n");
                }

                if (urlBuilder != null && urn != null) {
                    String url = (String) XposedHelpers.callMethod(urlBuilder, "a", new Class[]{XposedHelpers.findClass("dht", lpparam.classLoader)}, urn);
                    XposedMod.buildDownload(currentActivity, url, name);
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

            XposedHelpers.findAndHookConstructor("emx", lpparam.classLoader, XposedHelpers.findClass("avf", lpparam.classLoader), XposedHelpers.findClass("bkv", lpparam.classLoader), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedMod.urlBuilder = param.thisObject;
                }
            });

            XposedHelpers.findAndHookMethod("exo", lpparam.classLoader, "f", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedMod.addDownloadItem(XposedHelpers.getObjectField(param.thisObject, "b"));
                }
            });

            XposedHelpers.findAndHookMethod("hgu", lpparam.classLoader, "a", android.view.View.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.args[0];

                    int trackItemActions = XposedMod.currentActivity.getResources().getIdentifier("track_item_actions", "menu", "com.soundcloud.android");
                    int addToPlaylist = XposedMod.currentActivity.getResources().getIdentifier("add_to_playlist", "id", "com.soundcloud.android");
                    int removeFromPlaylist = XposedMod.currentActivity.getResources().getIdentifier("remove_from_playlist", "id", "com.soundcloud.android");
                    int goToArtist = XposedMod.currentActivity.getResources().getIdentifier("go_to_artist", "id", "com.soundcloud.android");

                    Object trackOverflowMenuActionsFactory = XposedHelpers.getObjectField(param.thisObject, "q");
                    Object track = XposedHelpers.getObjectField(param.thisObject, "r");
                    Object popupMenuWrapperFactory = XposedHelpers.getObjectField(param.thisObject, "a");
                    Object playlistOwnerUrn = XposedHelpers.getObjectField(param.thisObject, "u");
                    Object options = XposedHelpers.getObjectField(param.thisObject, "y");

                    Object from = XposedHelpers.callMethod(trackOverflowMenuActionsFactory, "a", track);
                    Object build = XposedHelpers.callMethod(popupMenuWrapperFactory, "a", view.getContext(), view);
                    XposedHelpers.callMethod(build, "a", trackItemActions);
                    XposedHelpers.callMethod(build, "a", param.thisObject);
                    XposedHelpers.callMethod(build, "b", param.thisObject);
                    XposedHelpers.callMethod(build, "a", addToPlaylist, XposedHelpers.callMethod(from, "d"));
                    XposedHelpers.callMethod(build, "a", removeFromPlaylist, XposedHelpers.callMethod(param.thisObject, "c", playlistOwnerUrn));
                    XposedHelpers.callMethod(build, "a", goToArtist, XposedHelpers.callMethod(options, "a"));
                    XposedHelpers.callMethod(param.thisObject, "a", from, view.getContext(), build);
                    XposedHelpers.callMethod(param.thisObject, "a", from, build);
                    XposedHelpers.callMethod(param.thisObject, "b", from, build);
                    XposedHelpers.callMethod(param.thisObject, "a", track, build, view.getContext());
                    XposedHelpers.callMethod(param.thisObject, "a", from, track, build);
                    XposedMod.addDownloadItem(build);
                    XposedHelpers.callMethod(build, "a");

                    return null;
                }
            });

            XposedHelpers.findAndHookMethod("exo", lpparam.classLoader, "a", android.view.MenuItem.class, android.content.Context.class, downloadCatcher);
            XposedHelpers.findAndHookMethod("hgu", lpparam.classLoader, "a", android.view.MenuItem.class, android.content.Context.class, downloadCatcher);

        } catch (Throwable t) {
            XposedBridge.log("[SoundCloud Downloader] Error: " + t.getMessage());
            return;
        }

        XposedBridge.log("[SoundCloud Downloader] Initialized");
    }
}
