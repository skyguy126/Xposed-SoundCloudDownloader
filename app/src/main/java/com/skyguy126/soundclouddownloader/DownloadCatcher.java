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

        // (HOOK) if: TrackPageMenuController, we need to get TrackItem
        // (HOOK) else: TrackItemMenuPresenter, already is TrackItem
        if (currentClassName.equals(XposedMod.trackPageMenuControllerHook)) {
            Object playerTrackState = XposedHelpers.getObjectField(param.thisObject, "l"); // (HOOK) PlayerTrackState -> fzy
            Object optionalTrackItem = XposedHelpers.callMethod(playerTrackState, "c"); // (HOOK) Optional -> itm<idi>
            track = XposedHelpers.callMethod(optionalTrackItem, "c"); // (HOOK) TrackItem -> idi
        } else {
            track = XposedHelpers.getObjectField(param.thisObject, "r"); // (HOOK) TrackItem -> idi
        }

        Object urn = XposedHelpers.callMethod(track, "m_"); // (HOOK) Call on TrackItem -> idi [Urn - dut]
        String name = (String) XposedHelpers.callMethod(track, "q"); // (HOOK) [Track - dvq] title()
        String artistName = (String) XposedHelpers.callMethod(track, "s"); // (HOOK) [Track - dvq] creatorName()

        XposedBridge.log("[SoundCloud Downloader] Track name: " + name);

        String genre = null;
        Object optionalGenre = XposedHelpers.callMethod(track, "p"); // (HOOK) [TrackItem - idi] genre()
        if ((boolean) XposedHelpers.callMethod(optionalGenre, "b")) {
            XposedBridge.log("[SoundCloud Downloader] Found genre data...");
            genre = (String) XposedHelpers.callMethod(optionalGenre, "c");
        }

        String imgUrl = null;
        Object optionalImg = XposedHelpers.callMethod(track, "b"); // (HOOK) [TrackItem - idi] getImageUrlTemplate()
        if ((boolean) XposedHelpers.callMethod(optionalImg, "b")) {
            imgUrl = (String) XposedHelpers.callMethod(optionalImg, "c");
            imgUrl = imgUrl.replaceAll("(\\{size\\})", "t250x250");
            XposedBridge.log("[SoundCloud Downloader] Image url: " + imgUrl);
        }

        if (XposedMod.urlBuilder != null && urn != null) {
            // (HOOK) StreamUrlBuilder - buildHttpsStreamUrl() [Method parameter class: Urn - dut]
            String url = (String) XposedHelpers.callMethod(XposedMod.urlBuilder, "a", new Class[]{XposedHelpers.findClass("dut", classLoader)}, urn);
            DownloadPayload downloadPayload = Downloader.buildDownloadPayload(url, name, imgUrl, artistName, genre);
            if (downloadPayload == null) return;
            Downloader.download(downloadPayload);
        } else {
            Toast.makeText(XposedMod.currentActivity, "Failed to fetch url!", Toast.LENGTH_SHORT).show();
        }

        param.setResult(true);
    }
}
