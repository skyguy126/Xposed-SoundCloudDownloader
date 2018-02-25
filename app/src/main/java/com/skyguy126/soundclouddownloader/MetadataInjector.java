package com.skyguy126.soundclouddownloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import de.robv.android.xposed.XposedBridge;

public class MetadataInjector extends BroadcastReceiver {

    private DownloadPayload downloadPayload;

    public MetadataInjector(DownloadPayload downloadPayload) {
        this.downloadPayload = downloadPayload;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        XposedBridge.log("[SoundCloud Downloader] Entering album art downloader...");
        context.unregisterReceiver(this);

        XposedBridge.log("[SoundCloud Downloader] Unregistered receiver, starting Picasso...");
        Picasso.with(XposedMod.currentActivity).load(downloadPayload.getImageUrl()).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                try {
                    File downloadedFile = new File(downloadPayload.getSaveDirectory(), downloadPayload.getFileName());
                    if (!downloadedFile.exists()) {
                        XposedBridge.log("[SoundCloud Downloader] downloaded file does not exist, exiting...");
                        return;
                    }

                    TagOptionSingleton.getInstance().setAndroid(true);
                    AudioFile audioFile = AudioFileIO.read(downloadedFile);
                    Tag tag = audioFile.getTagOrCreateAndSetDefault();
                    tag.setField(FieldKey.ARTIST,downloadPayload.getArtistName());
                    tag.setField(FieldKey.GENRE, downloadPayload.getGenre());
                    tag.setField(FieldKey.TITLE, downloadPayload.getTitle());
                    tag.setField(FieldKey.ALBUM, downloadPayload.getTitle());

                    ByteArrayOutputStream imgStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, imgStream);
                    File artworkFile = File.createTempFile(downloadPayload.getTitle(), ".jpg");
                    FileOutputStream artworkFileStream = new FileOutputStream(artworkFile);
                    artworkFileStream.write(imgStream.toByteArray());
                    artworkFileStream.close();
                    artworkFile.deleteOnExit();
                    imgStream.close();

                    Artwork artwork = ArtworkFactory.createArtworkFromFile(artworkFile);
                    artwork.setPictureType(3);
                    tag.deleteArtworkField();
                    tag.addField(artwork);
                    tag.setField(artwork);

                    audioFile.setTag(tag);
                    AudioFileIO.write(audioFile);

                    XposedBridge.log("[SoundCloud Downloader] Finished writing metadata...");

                    XposedMod.currentActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(audioFile.getFile())));

                    XposedBridge.log("[SoundCloud Downloader] Broadcasting update to media scanner...");

                } catch (Exception e) {
                    XposedBridge.log("[SoundCloud Downloader] Metadata error: " + e.getMessage());
                }
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                XposedBridge.log("[SoundCloud Downloader] Album art fetch failed!");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                XposedBridge.log("[SoundCloud Downloader] Preparing to load picasso...");
            }
        });
    }
}
