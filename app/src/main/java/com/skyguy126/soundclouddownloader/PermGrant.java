package com.skyguy126.soundclouddownloader;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import android.Manifest;

// Help from - https://github.com/Lawiusz/xposed_lockscreen_visualizer/blob/master/app/src/main/java/pl/lawiusz/lockscreenvisualizerxposed/PermGrant.java

public class PermGrant {

    private static final String CLASS_PACKAGE_MANAGER_SERVICE = "com.android.server.pm.PackageManagerService";
    private static final String CLASS_PACKAGE_PARSER_PACKAGE = "android.content.pm.PackageParser.Package";

    public static void initPerms(final ClassLoader loader) {
        try {
            final Class<?> pmServiceClass = XposedHelpers.findClass(CLASS_PACKAGE_MANAGER_SERVICE, loader);
            XposedHelpers.findAndHookMethod(pmServiceClass, "grantPermissionsLPw", CLASS_PACKAGE_PARSER_PACKAGE, boolean.class, String.class, new XC_MethodHook() {

                @Override
                @SuppressWarnings("unchecked")
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final String pkgName = (String) XposedHelpers.getObjectField(param.args[0], "packageName");

                    if (!pkgName.equals("com.soundcloud.android"))
                        return; // so we don't add random permissions to all apps

                    final Object extras = XposedHelpers.getObjectField(param.args[0], "mExtras");
                    final Object ps = XposedHelpers.callMethod(extras, "getPermissionsState");

                    final List<String> grantedPerms = (List<String>) XposedHelpers.getObjectField(param.args[0], "requestedPermissions");
                    final Object settings = XposedHelpers.getObjectField(param.thisObject, "mSettings");
                    final Object permissions = XposedHelpers.getObjectField(settings, "mPermissions");

                    if (!grantedPerms.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        final Object pWriteExternal = XposedHelpers.callMethod(permissions, "get", Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        XposedHelpers.callMethod(ps, "grantInstallPermission", pWriteExternal);
                    }

                    if (!grantedPerms.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        final Object pReadExternal = XposedHelpers.callMethod(permissions, "get", Manifest.permission.READ_EXTERNAL_STORAGE);
                        XposedHelpers.callMethod(ps, "grantInstallPermission", pReadExternal);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[SoundCloud Downloader] PermGrant Error: " + t.getMessage());
        }
    }
}
