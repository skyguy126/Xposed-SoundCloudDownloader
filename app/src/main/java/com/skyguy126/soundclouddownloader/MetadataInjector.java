package com.skyguy126.soundclouddownloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.cmc.music.metadata.ImageData;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import de.robv.android.xposed.XposedBridge;

public class MetadataInjector extends BroadcastReceiver {

    private DownloadPayload downloadPayload;

    public MetadataInjector(DownloadPayload downloadPayload) {
        this.downloadPayload = downloadPayload;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        XposedBridge.log("[SoundCloud Downloader] Entering album art downloader...");
        context.unregisterReceiver(this);

        XposedBridge.log("[SoundCloud Downloader] Unregistered dlrec, starting Picasso...");
        Picasso.with(XposedMod.currentActivity).load(downloadPayload.getImageUrl()).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                try {
                    File downloadedFile = new File(downloadPayload.getSaveDirectory(), downloadPayload.getFileName());
                    if (!downloadedFile.exists()) {
                        XposedBridge.log("[SoundCloud Downloader] downloaded file does not exist, exiting...");
                        return;
                    }

                    MusicMetadataSet metaSet = new MyID3().read(downloadedFile);
                    if (metaSet == null) {
                        XposedBridge.log("[SoundCloud Downloader] metaset is null...");
                        return;
                    }

                    MusicMetadata meta = new MusicMetadata("metadata");
                    meta.setArtist(downloadPayload.getArtistName());
                    meta.setGenre(downloadPayload.getGenre());
                    ByteArrayOutputStream imgStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, imgStream);
                    meta.addPicture(new ImageData(imgStream.toByteArray(), "", "", 3));
                    imgStream.close();

                    File taggedFile = File.createTempFile(downloadPayload.getTitle(), "-scd");
                    new MyID3().write(downloadedFile, taggedFile, metaSet, meta);

                    FileOutputStream downloadedFileStream = new FileOutputStream(downloadedFile, false);
                    byte[] taggedFileBytes = new byte[(int) taggedFile.length()];
                    FileInputStream taggedFileStream = new FileInputStream(taggedFile);
                    taggedFileStream.read(taggedFileBytes);
                    downloadedFileStream.write(taggedFileBytes);

                    downloadedFileStream.close();
                    taggedFileStream.close();
                    taggedFile.deleteOnExit();

                    XposedBridge.log("[SoundCloud Downloader] wrote mp3...");

                } catch (Exception e) {
                    XposedBridge.log("[SoundCloud Downloader] Metadata error: " + e.getMessage());
                }
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                XposedBridge.log("[SoundCloud Downloader] album art fetch failed!");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        });
    }
}
