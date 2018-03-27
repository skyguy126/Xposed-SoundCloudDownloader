package com.skyguy126.soundclouddownloader;

import android.view.MenuItem;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class DownloadCatcher extends XC_MethodHook {

    private ClassLoader classLoader;

    public DownloadCatcher(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        XposedBridge.log("[SoundCloud Downloader] Entered download catcher...");

        MenuItem item = (MenuItem) param.args[0];
        if (!item.getTitle().toString().equalsIgnoreCase("Download"))
            return;

        String currentClassName = param.thisObject.getClass().getName();
        Object track;

        // if: TrackPageMenuController -> fbu, we need to get TrackItem -> hlt
        // else: TrackItemMenuPresenter -> hlu, already is TrackItem -> hlt
        if (currentClassName.equals("fbu")) {
            Object playerTrackState = XposedHelpers.getObjectField(param.thisObject, "l"); // PlayerTrackState -> ewk
            Object optionalTrackItem = XposedHelpers.callMethod(playerTrackState, "a"); // Optional -> hxg
            track = XposedHelpers.callMethod(optionalTrackItem, "c"); // TrackItem -> hlt
        } else {
            track = XposedHelpers.getObjectField(param.thisObject, "r"); // TrackItem -> hlt
        }

        Object urn = XposedHelpers.callMethod(track, "getUrn");
        String name = (String) XposedHelpers.callMethod(track, "n");
        String artistName = (String) XposedHelpers.callMethod(track, "p");

        XposedBridge.log("[SoundCloud Downloader] Track name: " + name);

        String genre = null;
        Object optionalGenre = XposedHelpers.callMethod(track, "m");
        if ((boolean) XposedHelpers.callMethod(optionalGenre, "b")) {
            XposedBridge.log("[SoundCloud Downloader] Found genre data...");
            genre = (String) XposedHelpers.callMethod(optionalGenre, "c");
        }

        String imgUrl = null;
        Object optionalImg = XposedHelpers.callMethod(track, "getImageUrlTemplate");
        if ((boolean) XposedHelpers.callMethod(optionalImg, "b")) {
            imgUrl = (String) XposedHelpers.callMethod(optionalImg, "c");
            imgUrl = imgUrl.replaceAll("(\\{size\\})", "t250x250");
            XposedBridge.log("[SoundCloud Downloader] Image url: " + imgUrl);
        }

        if (XposedMod.urlBuilder != null && urn != null) {
            String url = (String) XposedHelpers.callMethod(XposedMod.urlBuilder, "a", new Class[]{XposedHelpers.findClass("dlb", classLoader)}, urn);
            DownloadPayload downloadPayload = Downloader.buildDownloadPayload(url, name, imgUrl, artistName, genre);
            if (downloadPayload == null) return;
            Downloader.download(downloadPayload);
        } else {
            Toast.makeText(XposedMod.currentActivity, "Failed to fetch url!", Toast.LENGTH_SHORT).show();
        }

        param.setResult(true);
    }
}
