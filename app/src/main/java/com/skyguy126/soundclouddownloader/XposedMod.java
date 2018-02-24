package com.skyguy126.soundclouddownloader;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedMod implements IXposedHookLoadPackage {

    //TODO - will cause memory leak, find workaround
    public static volatile Activity currentActivity;
    public static volatile Object urlBuilder;

    private static void addDownloadItem(Object popupMenuWrapper) {
        Object popupMenu = XposedHelpers.getObjectField(popupMenuWrapper, "a");
        Object menu = XposedHelpers.callMethod(popupMenu, "getMenu");
        XposedHelpers.callMethod(menu, "add", "Download");
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("com.soundcloud.android"))
            return;

        XposedBridge.log("[SoundCloud Downloader] Module entry");
        DownloadCatcher downloadCatcher = new DownloadCatcher(lpparam.classLoader);

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
